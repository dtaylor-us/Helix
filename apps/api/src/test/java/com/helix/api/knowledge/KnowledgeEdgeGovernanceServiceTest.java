package com.helix.api.knowledge;

import com.helix.api.knowledge.adapter.out.persistence.KnowledgeEdgeRepository;
import com.helix.api.knowledge.application.KnowledgeEdgeGovernanceService;
import com.helix.api.knowledge.domain.KnowledgeEdgeConfidence;
import com.helix.api.knowledge.domain.KnowledgeEdgeEntity;
import com.helix.api.knowledge.domain.KnowledgeEdgeOrigin;
import com.helix.api.knowledge.domain.KnowledgeEdgeStatus;
import com.helix.api.knowledge.domain.KnowledgeEdgeType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class KnowledgeEdgeGovernanceServiceTest {

    private final KnowledgeEdgeRepository edgeRepository = Mockito.mock(KnowledgeEdgeRepository.class);
    private final KnowledgeEdgeGovernanceService service = new KnowledgeEdgeGovernanceService(edgeRepository);

    private KnowledgeEdgeEntity proposedEdge() {
        return new KnowledgeEdgeEntity(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            KnowledgeEdgeType.BELIEF_SUPPORTED_BY_EVIDENCE, KnowledgeEdgeOrigin.AI_PROPOSED,
            KnowledgeEdgeStatus.PROPOSED, KnowledgeEdgeConfidence.MODERATE, "AI-proposed relationship", OffsetDateTime.now());
    }

    @Test
    void confirmMarksTheEdgeConfirmedAndSetsConfirmedAt() {
        var edge = proposedEdge();
        when(edgeRepository.findById(edge.getId())).thenReturn(Optional.of(edge));
        when(edgeRepository.save(edge)).thenReturn(edge);

        var result = service.confirm(edge.getId());

        assertEquals(KnowledgeEdgeStatus.CONFIRMED, result.getStatus());
        assertNotNull(result.getConfirmedAt());
        Mockito.verify(edgeRepository).save(edge);
    }

    @Test
    void rejectMarksTheEdgeRejectedAndSetsRejectedAt() {
        var edge = proposedEdge();
        when(edgeRepository.findById(edge.getId())).thenReturn(Optional.of(edge));
        when(edgeRepository.save(edge)).thenReturn(edge);

        var result = service.reject(edge.getId());

        assertEquals(KnowledgeEdgeStatus.REJECTED, result.getStatus());
        assertNotNull(result.getRejectedAt());
    }

    @Test
    void hideMarksTheEdgeHidden() {
        var edge = proposedEdge();
        when(edgeRepository.findById(edge.getId())).thenReturn(Optional.of(edge));
        when(edgeRepository.save(edge)).thenReturn(edge);

        var result = service.hide(edge.getId());

        assertEquals(KnowledgeEdgeStatus.HIDDEN, result.getStatus());
    }

    @Test
    void confirmThrowsWhenEdgeDoesNotExist() {
        var missingId = UUID.randomUUID();
        when(edgeRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.confirm(missingId));
    }
}
