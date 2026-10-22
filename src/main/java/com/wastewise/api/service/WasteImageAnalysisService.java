package com.wastewise.api.service;

import com.wastewise.api.model.ImagePrediction;
import com.wastewise.api.model.WasteImageAnalysis;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class WasteImageAnalysisService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 20_000_000L;

    private final HuggingFaceImageClassifier classifier;
    private final Path uploadDirectory;

    public WasteImageAnalysisService(
            HuggingFaceImageClassifier classifier,
            @Value("${app.uploads.directory:uploads/waste-images}") String uploadDirectory) {
        this.classifier = classifier;
        this.uploadDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    public WasteImageAnalysis analyze(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a photo before starting the scan.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Images must be 10 MB or smaller.");
        }

        // Inspect the actual image bytes rather than trusting the browser-supplied MIME type.
        ImageDimensions dimensions = inspectImage(file);
        String contentType = dimensions.contentType();
        String extension = contentType.equals("image/png") ? ".png" : ".jpg";
        String id = UUID.randomUUID().toString();
        String storedFilename = id + extension;
        Path storedPath = uploadDirectory.resolve(storedFilename).normalize();
        if (!storedPath.startsWith(uploadDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid upload path.");
        }

        byte[] imageBytes;
        try {
            imageBytes = file.getBytes();
            Files.createDirectories(uploadDirectory);
            Files.write(storedPath, imageBytes);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The image could not be stored on disk.", exception);
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "waste-image" : file.getOriginalFilename());
        String imageUrl = "/api/uploads/" + storedFilename;
        Instant analyzedAt = Instant.now();

        // Keep upload/storage usable even when local model inference is disabled.
        if (!classifier.isConfigured()) {
            return baseResponse(id, originalFilename, storedFilename, imageUrl, contentType, file.getSize(), dimensions,
                    false, null, null, null, null, null, null, List.of(), List.of(), List.of(),
                    "Image saved successfully. Set HF_VISION_ENABLED=true in the project-root .env file and restart Spring Boot to enable AI predictions.", analyzedAt);
        }

        try {
            List<ImagePrediction> predictions = classifier.classify(imageBytes);
            ImagePrediction best = predictions.get(0);
            WasteAdvice advice = adviceFor(best.label());
            String message = best.score() < 0.45
                    ? "The model is not very confident. Check the result manually and follow local collection rules."
                    : "Image classified. Use this as a helpful starting point, then confirm local disposal rules.";
            return baseResponse(id, originalFilename, storedFilename, imageUrl, contentType, file.getSize(), dimensions,
                    true, best.label(), best.score(), advice.category(), advice.categorySlug(), advice.recyclability(), advice.disposalType(),
                    advice.instructions(), advice.avoid(), predictions, message, analyzedAt);
        } catch (RuntimeException exception) {
            return baseResponse(id, originalFilename, storedFilename, imageUrl, contentType, file.getSize(), dimensions,
                    false, null, null, null, null, null, null, List.of(), List.of(), List.of(),
                    "Image saved, but AI analysis did not complete. Check model download access and network connectivity, then retry with another clear photo.", analyzedAt);
        }
    }

    private ImageDimensions inspectImage(MultipartFile file) {
        try (InputStream input = file.getInputStream(); ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is not a readable image.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is not a supported JPG or PNG image.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String detectedContentType;
                if (format.equals("png")) {
                    detectedContentType = "image/png";
                } else if (format.equals("jpeg") || format.equals("jpg")) {
                    detectedContentType = "image/jpeg";
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload a JPG, JPEG, or PNG image.");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The image dimensions are too large. Use an image under 20 megapixels.");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file could not be decoded as an image.");
                }
                decoded.flush();
                return new ImageDimensions(width, height, detectedContentType);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded image could not be read.", exception);
        }
    }

    private WasteImageAnalysis baseResponse(String id, String originalFilename, String storedFilename, String imageUrl,
            String contentType, long fileSizeBytes, ImageDimensions dimensions, boolean analyzed, String predictedLabel,
            Double confidence, String category, String categorySlug, String recyclability, String disposalType,
            List<String> instructions, List<String> avoid, List<ImagePrediction> predictions, String message, Instant analyzedAt) {
        return new WasteImageAnalysis(id, originalFilename, storedFilename, imageUrl, contentType, fileSizeBytes,
                dimensions.width(), dimensions.height(), analyzed, predictedLabel, confidence, category, categorySlug,
                recyclability, disposalType, instructions, avoid, predictions, message, analyzedAt);
    }

    private WasteAdvice adviceFor(String rawLabel) {
        String label = rawLabel.toLowerCase(Locale.ROOT).replace('_', '-').trim();
        return switch (label) {
            case "battery" -> new WasteAdvice("Hazardous waste", "hazardous", "Special collection", "Authorized battery collection",
                    List.of("Keep the battery dry and away from heat.", "Tape exposed terminals where practical, especially on loose lithium batteries.", "Take it to an authorized battery or e-waste collection point."),
                    List.of("Do not put it in regular trash or mixed recycling.", "Do not crush, puncture, or burn the battery."));
            case "biological", "brown-grass", "vegetation", "food-organics" -> new WasteAdvice("Organic", "organic", "Usually compostable", "Compost / wet-waste collection",
                    List.of("Place it in your local organic-waste collection or suitable compost system.", "Remove plastic wrappers and other non-compostable materials.", "Follow your local rules for garden and food waste."),
                    List.of("Do not mix it with plastic, glass, or metal.", "Not every compost system accepts all food scraps."));
            case "cardboard" -> new WasteAdvice("Recyclables", "recyclables", "Often recyclable", "Paper and cardboard recycling",
                    List.of("Remove packing foam, plastic wrap, and food residue.", "Flatten the cardboard and keep it dry.", "Place it in the paper/cardboard recycling stream."),
                    List.of("Do not recycle greasy or heavily food-soiled cardboard with clean paper."));
            case "clothes", "shoes", "textile-trash", "textile" -> new WasteAdvice("Textiles", "textiles", "Reuse first", "Donation or textile collection",
                    List.of("Check whether the item is clean and usable.", "Donate or repair it if practical.", "Use a textile collection point for worn-out items where available."),
                    List.of("Do not donate wet, mouldy, or heavily soiled textiles."));
            case "green-glass", "white-glass", "glass", "brown-glass" -> new WasteAdvice("Recyclables", "recyclables", "Location-dependent", "Glass collection",
                    List.of("Empty and rinse the container if practical.", "Keep broken glass safely contained to prevent injury.", "Use a designated glass collection point and follow local colour-sorting rules."),
                    List.of("Do not place loose broken glass where it can injure collection workers."));
            case "metal" -> new WasteAdvice("Recyclables", "recyclables", "Often recyclable", "Metal recycling",
                    List.of("Empty and rinse the container if it held food or drink.", "Keep sharp edges contained.", "Place it in the metal-recycling stream if accepted locally."),
                    List.of("Do not place pressurised or chemical containers in regular recycling."));
            case "paper" -> new WasteAdvice("Recyclables", "recyclables", "Often recyclable", "Paper recycling",
                    List.of("Keep paper dry and free of food or oil.", "Remove non-paper components where required.", "Place it in paper recycling if accepted by your local program."),
                    List.of("Do not place wet or oil-soaked paper with clean paper recycling."));
            case "plastic" -> new WasteAdvice("Recyclables", "recyclables", "Depends on plastic type", "Local plastic recycling",
                    List.of("Empty and rinse the container.", "Check the resin code and local collection rules.", "Use the recycling stream only if that plastic type is accepted."),
                    List.of("Do not assume every kind of plastic is recyclable locally."));
            case "trash", "garbage", "miscellaneous-trash", "general-waste" -> new WasteAdvice("General waste", "general", "Not usually recyclable", "Residual waste bin",
                    List.of("Check whether the item can be reused or repaired first.", "Separate any battery or electronic parts if safe to do so.", "Use the local residual-waste collection."),
                    List.of("Do not put batteries, chemicals, or electronics in general waste."));
            default -> new WasteAdvice("Needs review", "unknown", "Uncertain", "Check local waste guidance",
                    List.of("The model label is not mapped to a disposal category yet.", "Compare the image with the waste guide or consult your local authority."),
                    List.of("Do not rely on an uncertain prediction for hazardous items."));
        };
    }

    private record ImageDimensions(int width, int height, String contentType) { }
    private record WasteAdvice(String category, String categorySlug, String recyclability, String disposalType,
                               List<String> instructions, List<String> avoid) { }
}
