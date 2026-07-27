package com.helix.api.shared.application;

import com.helix.api.beliefs.application.BeliefService;
import com.helix.api.evidence.application.EvidenceService;
import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.wisdom.application.WeeklyRetrospectiveService;
import com.helix.api.wisdom.application.WisdomService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StructuredSearchService {

    private final ReflectionService reflectionService;
    private final BeliefService beliefService;
    private final EvidenceService evidenceService;
    private final WisdomService wisdomService;
    private final WeeklyRetrospectiveService retrospectiveService;
    private final SemanticIndexingService semanticIndexingService;
    private final SemanticRetrievalService semanticRetrievalService;

    public StructuredSearchService(ReflectionService reflectionService,
                                   BeliefService beliefService,
                                   EvidenceService evidenceService,
                                   WisdomService wisdomService,
                                   WeeklyRetrospectiveService retrospectiveService,
                                   SemanticIndexingService semanticIndexingService,
                                   SemanticRetrievalService semanticRetrievalService) {
        this.reflectionService = reflectionService;
        this.beliefService = beliefService;
        this.evidenceService = evidenceService;
        this.wisdomService = wisdomService;
        this.retrospectiveService = retrospectiveService;
        this.semanticIndexingService = semanticIndexingService;
        this.semanticRetrievalService = semanticRetrievalService;
    }

    public List<SearchRecord> search(String query) {
        var normalized = query == null ? "" : query.trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        if (!semanticIndexingService.isIndexed()) {
            semanticIndexingService.rebuild();
        }

        var results = new ArrayList<SearchRecord>();
        reflectionService.search(normalized).forEach(item ->
            results.add(new SearchRecord(
                "REFLECTION",
                item.getId(),
                item.getContent(),
                item.getCreatedAt().toString(),
                "KEYWORD",
                1.0
            ))
        );
        beliefService.search(normalized).forEach(item ->
            results.add(new SearchRecord(
                "BELIEF",
                item.getId(),
                item.getStatement(),
                item.getRevisedAt().toString(),
                "KEYWORD",
                1.0
            ))
        );
        evidenceService.search(normalized).forEach(item ->
            results.add(new SearchRecord(
                "EVIDENCE",
                item.getId(),
                item.getSummary(),
                item.getCreatedAt().toString(),
                "KEYWORD",
                1.0
            ))
        );
        wisdomService.search(normalized).forEach(item ->
            results.add(new SearchRecord(
                "WISDOM",
                item.getId(),
                item.getStatement(),
                item.getRevisedAt().toString(),
                "KEYWORD",
                1.0
            ))
        );
        retrospectiveService.search(normalized).forEach(item ->
            results.add(new SearchRecord(
                "RETROSPECTIVE",
                item.getId(),
                item.getSummary(),
                item.getCreatedAt().toString(),
                "KEYWORD",
                1.0
            ))
        );

        semanticRetrievalService.retrieve(normalized, 15).forEach(item ->
            results.add(new SearchRecord(
                item.recordType(),
                item.recordId(),
                item.snippet(),
                item.createdAt(),
                "SEMANTIC",
                item.score()
            ))
        );

        return deduplicate(results);
    }

    private List<SearchRecord> deduplicate(List<SearchRecord> records) {
        Map<String, SearchRecord> byRecord = records.stream()
            .collect(Collectors.toMap(
                record -> record.recordType() + ":" + record.recordId(),
                record -> record,
                (left, right) -> {
                    if (!left.matchType().equals(right.matchType())) {
                        return new SearchRecord(
                            left.recordType(),
                            left.recordId(),
                            left.snippet(),
                            left.createdAt(),
                            "HYBRID",
                            Math.max(left.score(), right.score())
                        );
                    }
                    return left.score() >= right.score() ? left : right;
                }
            ));

        return byRecord.values().stream()
            .sorted((left, right) -> Double.compare(right.score(), left.score()))
            .toList();
    }

    public record SearchRecord(String recordType, UUID recordId, String snippet, String createdAt, String matchType,
                               double score) {}
}
