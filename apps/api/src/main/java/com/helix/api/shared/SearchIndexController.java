package com.helix.api.shared;

import com.helix.api.shared.application.SemanticIndexingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SearchIndexController {

    private final SemanticIndexingService semanticIndexingService;

    public SearchIndexController(SemanticIndexingService semanticIndexingService) {
        this.semanticIndexingService = semanticIndexingService;
    }

    @PostMapping("/api/v1/search/index/rebuild")
    public Map<String, Object> rebuild() {
        var result = semanticIndexingService.rebuild();
        return Map.of(
            "indexedCount", result.indexedCount(),
            "embeddingModel", result.embeddingModel(),
            "indexedAt", result.indexedAt()
        );
    }
}