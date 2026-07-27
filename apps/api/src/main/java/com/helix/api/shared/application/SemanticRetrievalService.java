package com.helix.api.shared.application;

import com.helix.api.shared.adapter.out.persistence.SemanticSearchDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class SemanticRetrievalService {

    private final SemanticSearchDocumentRepository repository;
    private final TextEmbeddingPort textEmbeddingPort;

    public SemanticRetrievalService(SemanticSearchDocumentRepository repository, TextEmbeddingPort textEmbeddingPort) {
        this.repository = repository;
        this.textEmbeddingPort = textEmbeddingPort;
    }

    public List<SemanticMatch> retrieve(String query, int limit) {
        var normalized = query == null ? "" : query.trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        var queryEmbedding = textEmbeddingPort.embed(normalized);
        var matches = new ArrayList<SemanticMatch>();

        repository.findAllByOrderByIndexedAtDesc().forEach(document -> {
            var similarity = cosineSimilarity(queryEmbedding, parse(document.getEmbeddingValues()));
            if (similarity > 0d) {
                matches.add(new SemanticMatch(
                    document.getRecordType(),
                    document.getRecordId(),
                    document.getSnippet(),
                    document.getSourceUpdatedAt().toString(),
                    similarity
                ));
            }
        });

        return matches.stream()
            .sorted(Comparator.comparingDouble(SemanticMatch::score).reversed())
            .limit(limit)
            .toList();
    }

    private static List<Double> parse(String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return List.of();
        }
        var values = serialized.split(",");
        var output = new ArrayList<Double>(values.length);
        for (String value : values) {
            output.add(Double.parseDouble(value));
        }
        return output;
    }

    private static double cosineSimilarity(List<Double> left, List<Double> right) {
        if (left.isEmpty() || right.isEmpty() || left.size() != right.size()) {
            return 0d;
        }

        double dot = 0d;
        double leftNorm = 0d;
        double rightNorm = 0d;
        for (int i = 0; i < left.size(); i++) {
            double lv = left.get(i);
            double rv = right.get(i);
            dot += lv * rv;
            leftNorm += lv * lv;
            rightNorm += rv * rv;
        }

        if (leftNorm == 0d || rightNorm == 0d) {
            return 0d;
        }

        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    public record SemanticMatch(String recordType, UUID recordId, String snippet, String createdAt, double score) {
    }
}