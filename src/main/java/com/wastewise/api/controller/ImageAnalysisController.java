package com.wastewise.api.controller;

import com.wastewise.api.model.WasteImageAnalysis;
import com.wastewise.api.service.WasteImageAnalysisService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ImageAnalysisController {
    private final WasteImageAnalysisService imageAnalysisService;

    public ImageAnalysisController(WasteImageAnalysisService imageAnalysisService) {
        this.imageAnalysisService = imageAnalysisService;
    }

    @PostMapping(value = "/analyze-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public WasteImageAnalysis analyzeImage(@RequestParam("image") MultipartFile image) {
        return imageAnalysisService.analyze(image);
    }
}
