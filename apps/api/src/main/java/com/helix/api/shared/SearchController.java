package com.helix.api.shared;

import com.helix.api.shared.application.StructuredSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SearchController {

    private final StructuredSearchService structuredSearchService;

    public SearchController(StructuredSearchService structuredSearchService) {
        this.structuredSearchService = structuredSearchService;
    }

    @GetMapping("/api/v1/search")
    public Map<String, Object> search(@RequestParam(name = "q", required = false) String query) {
        List<StructuredSearchService.SearchRecord> results = structuredSearchService.search(query);
        return Map.of(
            "query", query,
            "results", results,
            "note", "Hybrid keyword and semantic retrieval is active with source citations"
        );
    }
}
