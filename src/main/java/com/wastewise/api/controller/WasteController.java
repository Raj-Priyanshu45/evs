package com.wastewise.api.controller;

import com.wastewise.api.model.WasteCategory;
import com.wastewise.api.model.WasteItem;
import com.wastewise.api.service.WasteCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WasteController {
    private final WasteCatalogService catalogService;

    public WasteController(WasteCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "waste-wise-api");
    }

    @GetMapping("/categories")
    public List<WasteCategory> categories() {
        return catalogService.getCategories();
    }

    @GetMapping("/waste-items")
    public List<WasteItem> wasteItems(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "all") String category) {
        return catalogService.search(q, category);
    }

    @GetMapping("/waste-items/{id}")
    public WasteItem wasteItem(@PathVariable String id) {
        return catalogService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Waste item not found: " + id));
    }
}
