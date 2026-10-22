package com.wastewise.api;

import com.wastewise.api.model.WasteItem;
import com.wastewise.api.service.WasteCatalogService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WasteCatalogServiceTest {
    private final WasteCatalogService service = new WasteCatalogService();

    @Test
    void returnsSeedCatalog() {
        assertThat(service.search("", "all")).hasSize(12);
    }

    @Test
    void filtersHazardousWasteAndSearchTerm() {
        assertThat(service.search("battery", "hazardous"))
                .extracting(WasteItem::id)
                .containsExactly("used-battery");
    }

    @Test
    void returnsEmptyForUnknownItem() {
        assertThat(service.findById("not-a-real-item")).isEmpty();
    }
}
