package com.helix.api.shared;

import com.helix.api.shared.adapter.out.persistence.SemanticSearchDocumentRepository;
import com.helix.api.shared.application.SemanticRetrievalService;
import com.helix.api.shared.application.TextEmbeddingPort;
import com.helix.api.shared.domain.SemanticSearchDocumentEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class SemanticRetrievalServiceTest {

    @Test
    void retrieveFiltersOutLowConfidenceSemanticMatches() {
        var repository = Mockito.mock(SemanticSearchDocumentRepository.class);
        var textEmbeddingPort = Mockito.mock(TextEmbeddingPort.class);
        when(textEmbeddingPort.embed("search query")).thenReturn(List.of(1d, 0d));
        when(repository.findAllByOrderByIndexedAtDesc()).thenReturn(List.of(
            document("REFLECTION", "Weak match", "0.09,0.995941765365827"),
            document("WISDOM", "Strong match", "0.31,0.9507365565707464")
        ));

        var service = new SemanticRetrievalService(repository, textEmbeddingPort);

        var results = service.retrieve("search query", 15);

        assertEquals(1, results.size());
        assertEquals("WISDOM", results.getFirst().recordType());
        assertEquals("Strong match", results.getFirst().snippet());
        assertEquals(0.31d, results.getFirst().score());
    }

    private static SemanticSearchDocumentEntity document(String recordType, String snippet, String embeddingValues) {
        var now = OffsetDateTime.now();
        return new SemanticSearchDocumentEntity(
            UUID.randomUUID(),
            recordType,
            UUID.randomUUID(),
            snippet,
            embeddingValues,
            now,
            now
        );
    }
}
