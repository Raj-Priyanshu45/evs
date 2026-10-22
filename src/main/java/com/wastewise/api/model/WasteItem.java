package com.wastewise.api.model;

import java.util.List;

public record WasteItem(
        String id,
        String name,
        String description,
        String category,
        String categorySlug,
        String icon,
        String recyclability,
        String disposalType,
        List<String> instructions,
        List<String> avoid,
        String tip
) { }
