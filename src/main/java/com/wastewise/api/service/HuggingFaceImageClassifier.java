package com.wastewise.api.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import com.wastewise.api.model.ImagePrediction;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Downloads a public waste classifier from Hugging Face and runs it locally using ONNX Runtime. */
@Service
public class HuggingFaceImageClassifier {
    private static final int INPUT_SIZE = 224;
    private static final int PIXELS = INPUT_SIZE * INPUT_SIZE;
    private static final List<String> LABELS = List.of(
            "battery", "biological", "cardboard", "glass", "metal", "paper", "plastic", "trash");

    private final boolean enabled;
    private final String token;
    private final String modelId;
    private final String modelFile;
    private final Path modelDirectory;
    private final HttpClient httpClient;
    private final OrtEnvironment environment;
    private volatile OrtSession session;
    private volatile String inputName;

    public HuggingFaceImageClassifier(
            @Value("${app.ai.huggingface.enabled:true}") boolean enabled,
            @Value("${app.ai.huggingface.token:}") String token,
            @Value("${app.ai.huggingface.model-id:SriramRokkam/wastewise-garbage-cls}") String modelId,
            @Value("${app.ai.huggingface.model-file:wastewise-yolo.onnx}") String modelFile,
            @Value("${app.ai.huggingface.model-directory:models}") String modelDirectory) {
        this.enabled = enabled;
        this.token = token == null ? "" : token.trim();
        this.modelId = modelId == null ? "SriramRokkam/wastewise-garbage-cls" : modelId.trim();
        this.modelFile = modelFile == null ? "wastewise-yolo.onnx" : Paths.get(modelFile).getFileName().toString();
        this.modelDirectory = Paths.get(modelDirectory).toAbsolutePath().normalize();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.environment = OrtEnvironment.getEnvironment();
    }

    public boolean isConfigured() {
        return enabled && !modelId.isBlank() && !modelFile.isBlank();
    }

    public List<ImagePrediction> classify(byte[] imageBytes) {
        if (!isConfigured()) {
            throw new IllegalStateException("Hugging Face image analysis is disabled.");
        }
        try {
            ensureSession();
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (source == null) {
                throw new IllegalArgumentException("The uploaded file could not be decoded for image analysis.");
            }
            float[] input = toNchwTensor(source);
            source.flush();

            try (OnnxTensor tensor = OnnxTensor.createTensor(environment,
                    FloatBuffer.wrap(input), new long[]{1, 3, INPUT_SIZE, INPUT_SIZE});
                 OrtSession.Result result = session.run(Map.of(inputName, tensor))) {
                Object raw = result.get(0).getValue();
                double[] scores = unpackScores(raw);
                double[] probabilities = toProbabilities(scores);
                List<ImagePrediction> predictions = new ArrayList<>();
                for (int index = 0; index < Math.min(LABELS.size(), probabilities.length); index++) {
                    predictions.add(new ImagePrediction(LABELS.get(index), probabilities[index]));
                }
                return predictions.stream()
                        .sorted(Comparator.comparingDouble(ImagePrediction::score).reversed())
                        .limit(5)
                        .toList();
            }
        } catch (IOException | OrtException exception) {
            throw new IllegalStateException("The local waste-classification model could not process this image.", exception);
        }
    }

    private synchronized void ensureSession() throws IOException, OrtException {
        if (session != null) return;

        Files.createDirectories(modelDirectory);
        Path modelPath = modelDirectory.resolve(modelFile).normalize();
        if (!modelPath.startsWith(modelDirectory)) {
            throw new IllegalStateException("Invalid model file path.");
        }
        if (!Files.isRegularFile(modelPath) || Files.size(modelPath) < 1_000_000L) {
            downloadModel(modelPath);
        }

        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setIntraOpNumThreads(Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors())));
        try {
            OrtSession loaded;
            try {
                loaded = environment.createSession(modelPath.toString(), options);
            } catch (OrtException exception) {
                Files.deleteIfExists(modelPath);
                throw exception;
            }
            String selectedInput = loaded.getInputNames().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("The ONNX model has no input tensor."));
            if (!(loaded.getInputInfo().get(selectedInput).getInfo() instanceof TensorInfo)) {
                loaded.close();
                throw new IllegalStateException("The ONNX model input is not a tensor.");
            }
            this.inputName = selectedInput;
            this.session = loaded;
        } finally {
            options.close();
        }
    }

    private void downloadModel(Path modelPath) throws IOException {
        String safeModelId = modelId.replaceAll("[^A-Za-z0-9._/-]", "");
        URI modelUri = URI.create("https://huggingface.co/" + safeModelId + "/resolve/main/" + modelFile + "?download=true");
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(modelUri)
                .timeout(Duration.ofMinutes(3))
                .header("User-Agent", "WasteWise-Microproject/1.0")
                .GET();
        if (isRealToken(token)) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        Path temporaryPath = modelDirectory.resolve(modelFile + ".download");
        try {
            HttpResponse<byte[]> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Hugging Face model download failed with HTTP " + response.statusCode()
                        + ". Check HF_MODEL_ID, HF_MODEL_FILE, network access, and optional HF_TOKEN.");
            }
            byte[] modelBytes = response.body();
            if (modelBytes == null || modelBytes.length < 1_000_000L) {
                throw new IOException("The Hugging Face response was too small to be a valid ONNX model.");
            }
            Files.write(temporaryPath, modelBytes);
            try {
                Files.move(temporaryPath, modelPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                Files.move(temporaryPath, modelPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Model download was interrupted.", exception);
        } finally {
            Files.deleteIfExists(temporaryPath);
        }
    }

    private boolean isRealToken(String candidate) {
        String lower = candidate.toLowerCase(Locale.ROOT);
        return candidate.startsWith("hf_") && candidate.length() > 20
                && !lower.contains("replace") && !lower.contains("your_");
    }

    private float[] toNchwTensor(BufferedImage source) {
        BufferedImage resized = new BufferedImage(INPUT_SIZE, INPUT_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, INPUT_SIZE, INPUT_SIZE, null);
        } finally {
            graphics.dispose();
        }

        float[] values = new float[3 * PIXELS];
        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int pixel = resized.getRGB(x, y);
                int offset = y * INPUT_SIZE + x;
                values[offset] = ((pixel >> 16) & 0xff) / 255.0f;
                values[PIXELS + offset] = ((pixel >> 8) & 0xff) / 255.0f;
                values[(2 * PIXELS) + offset] = (pixel & 0xff) / 255.0f;
            }
        }
        resized.flush();
        return values;
    }

    private double[] unpackScores(Object raw) {
        if (raw instanceof float[][] matrix && matrix.length > 0) {
            double[] scores = new double[matrix[0].length];
            for (int index = 0; index < scores.length; index++) scores[index] = matrix[0][index];
            return scores;
        }
        if (raw instanceof float[] scores) {
            double[] result = new double[scores.length];
            for (int index = 0; index < scores.length; index++) result[index] = scores[index];
            return result;
        }
        if (raw instanceof double[][] matrix && matrix.length > 0) {
            return matrix[0];
        }
        if (raw instanceof double[] scores) {
            return scores;
        }
        throw new IllegalStateException("The ONNX model returned an unsupported output tensor: " + raw.getClass().getSimpleName());
    }

    private double[] toProbabilities(double[] scores) {
        boolean alreadyProbabilities = true;
        double sum = 0;
        for (double score : scores) {
            if (!Double.isFinite(score) || score < 0 || score > 1) alreadyProbabilities = false;
            sum += score;
        }
        if (alreadyProbabilities && Math.abs(sum - 1.0) < 0.08) return scores;

        double max = Double.NEGATIVE_INFINITY;
        for (double score : scores) max = Math.max(max, score);
        double[] result = new double[scores.length];
        double denominator = 0;
        for (int index = 0; index < scores.length; index++) {
            result[index] = Math.exp(scores[index] - max);
            denominator += result[index];
        }
        if (denominator == 0 || !Double.isFinite(denominator)) {
            throw new IllegalStateException("The model returned invalid prediction scores.");
        }
        for (int index = 0; index < result.length; index++) result[index] /= denominator;
        return result;
    }

    @PreDestroy
    public void close() {
        if (session != null) {
            try {
                session.close();
            } catch (OrtException ignored) {
                // Best-effort shutdown cleanup.
            }
        }
    }
}
