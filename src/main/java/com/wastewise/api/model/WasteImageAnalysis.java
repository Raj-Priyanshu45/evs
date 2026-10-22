package com.wastewise.api.model;

import java.time.Instant;
import java.util.List;

/** API response for an uploaded waste image and its AI-assisted assessment. */
public record WasteImageAnalysis(
        String id,
        String originalFilename,
        String storedFilename,
        String imageUrl,
        String contentType,
        long fileSizeBytes,
        int imageWidth,
        int imageHeight,
        boolean analyzed,
        String predictedLabel,
        Double confidence,
        String category,
        String categorySlug,
        String recyclability,
        String disposalType,
        List<String> instructions,
        List<String> avoid,
        List<ImagePrediction> topPredictions,
        String message,
        Instant analyzedAt
) { }
