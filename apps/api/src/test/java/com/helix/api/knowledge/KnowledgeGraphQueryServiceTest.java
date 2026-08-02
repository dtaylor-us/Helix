package com.helix.api.knowledge;

import com.helix.api.knowledge.adapter.out.persistence.KnowledgeEdgeRepository;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeEdgeSourceRepository;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeNodeRepository;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeProjectionCheckpointRepository;
import com.helix.api.knowledge.application.KnowledgeGraphQueryService;
import com.helix.api.knowledge.domain.KnowledgeEdgeConfidence;
import com.helix.api.knowledge.domain.KnowledgeEdgeEntity;
import com.helix.api.knowledge.domain.KnowledgeEdgeOrigin;
import com.helix.api.knowledge.domain.KnowledgeEdgeStatus;
import com.helix.api.knowledge.domain.KnowledgeEdgeType;
import com.helix.api.knowledge.domain.KnowledgeNodeEntity;
import com.helix.api.knowledge.domain.KnowledgeNodeType;
import com.helix.api.knowledge.domain.KnowledgeProjectionCheckpointEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class KnowledgeGraphQueryServiceTest {

    private final KnowledgeNodeRepository nodeRepository = Mockito.mock(KnowledgeNodeRepository.class);
    private final KnowledgeEdgeRepository edgeRepository = Mockito.mock(KnowledgeEdgeRepository.class);
    private final KnowledgeEdgeSourceRepository edgeSourceRepository = Mockito.mock(KnowledgeEdgeSourceRepository.class);
    private final KnowledgeProjectionCheckpointRepository checkpointRepository = Mockito.mock(KnowledgeProjectionCheckpointRepository.class);

    private final KnowledgeGraphQueryService service = new KnowledgeGraphQueryService(
        nodeRepository, edgeRepository, edgeSourceRepository, checkpointRepository
    );

    private KnowledgeNodeEntity node(KnowledgeNodeType type, UUID sourceRecordId) {
        return new KnowledgeNodeEntity(UUID.randomUUID(), type, sourceRecordId, "label", "summary", null,
            OffsetDateTime.now(), OffsetDateTime.now());
    }

    private KnowledgeEdgeEntity edge(UUID sourceNodeId, UUID targetNodeId, KnowledgeEdgeStatus status) {
        var e = new KnowledgeEdgeEntity(UUID.randomUUID(), sourceNodeId, targetNodeId,
            KnowledgeEdgeType.TRANSFORMATION_CONTAINS_BELIEF, KnowledgeEdgeOrigin.EXPLICIT_DOMAIN_RELATIONSHIP,
            status, KnowledgeEdgeConfidence.EXPLICIT, "explanation", OffsetDateTime.now());
        return e;
    }

    @Test
    void focusViewThrowsActionableErrorWhenFocusNodeIsNotProjected() {
        var sourceRecordId = UUID.randomUUID();
        when(nodeRepository.findByNodeTypeAndSourceRecordId(KnowledgeNodeType.TRANSFORMATION, sourceRecordId))
            .thenReturn(Optional.empty());

        var error = assertThrows(NoSuchElementException.class,
            () -> service.focusView(KnowledgeNodeType.TRANSFORMATION, sourceRecordId));

        assertTrue(error.getMessage().contains("rebuild"));
    }

    @Test
    void focusViewWalksConfirmedEdgesOutwardFromTheFocusNode() {
        var sourceRecordId = UUID.randomUUID();
        var focus = node(KnowledgeNodeType.TRANSFORMATION, sourceRecordId);
        var neighbor1 = node(KnowledgeNodeType.BELIEF, UUID.randomUUID());
        var neighbor2 = node(KnowledgeNodeType.EXPERIMENT, UUID.randomUUID());
        var confirmedEdge = edge(focus.getId(), neighbor1.getId(), KnowledgeEdgeStatus.CONFIRMED);
        var rejectedEdge = edge(focus.getId(), neighbor2.getId(), KnowledgeEdgeStatus.REJECTED);

        when(nodeRepository.findByNodeTypeAndSourceRecordId(KnowledgeNodeType.TRANSFORMATION, sourceRecordId))
            .thenReturn(Optional.of(focus));
        when(edgeRepository.findByStatus(KnowledgeEdgeStatus.CONFIRMED)).thenReturn(List.of(confirmedEdge));
        when(nodeRepository.findAllById(Mockito.any())).thenAnswer(inv -> List.of(focus, neighbor1));
        when(edgeSourceRepository.findByKnowledgeEdgeIdIn(Mockito.any())).thenReturn(List.of());

        var view = service.focusView(KnowledgeNodeType.TRANSFORMATION, sourceRecordId);

        assertEquals(focus, view.focusNode());
        assertEquals(2, view.nodes().size());
        assertEquals(1, view.edges().size());
        assertFalse(view.truncated());
        Mockito.verify(edgeRepository, Mockito.never()).findByStatus(KnowledgeEdgeStatus.REJECTED);
    }

    @Test
    void focusViewTruncatesWhenMaxNodesIsReached() {
        var sourceRecordId = UUID.randomUUID();
        var focus = node(KnowledgeNodeType.TRANSFORMATION, sourceRecordId);
        var neighbor1 = node(KnowledgeNodeType.BELIEF, UUID.randomUUID());
        var neighbor2 = node(KnowledgeNodeType.EXPERIMENT, UUID.randomUUID());
        var edge1 = edge(focus.getId(), neighbor1.getId(), KnowledgeEdgeStatus.CONFIRMED);
        var edge2 = edge(focus.getId(), neighbor2.getId(), KnowledgeEdgeStatus.CONFIRMED);

        when(nodeRepository.findByNodeTypeAndSourceRecordId(KnowledgeNodeType.TRANSFORMATION, sourceRecordId))
            .thenReturn(Optional.of(focus));
        when(edgeRepository.findByStatus(KnowledgeEdgeStatus.CONFIRMED)).thenReturn(List.of(edge1, edge2));
        when(nodeRepository.findAllById(Mockito.any())).thenAnswer(inv -> List.of(focus, neighbor1));
        when(edgeSourceRepository.findByKnowledgeEdgeIdIn(Mockito.any())).thenReturn(List.of());

        var view = service.focusView(KnowledgeNodeType.TRANSFORMATION, sourceRecordId, 2, 2);

        assertTrue(view.truncated());
    }

    @Test
    void focusViewRespectsMaxDepth() {
        var sourceRecordId = UUID.randomUUID();
        var focus = node(KnowledgeNodeType.TRANSFORMATION, sourceRecordId);
        var oneHop = node(KnowledgeNodeType.BELIEF, UUID.randomUUID());
        var twoHop = node(KnowledgeNodeType.EVIDENCE, UUID.randomUUID());
        var edge1 = edge(focus.getId(), oneHop.getId(), KnowledgeEdgeStatus.CONFIRMED);
        var edge2 = edge(oneHop.getId(), twoHop.getId(), KnowledgeEdgeStatus.CONFIRMED);

        when(nodeRepository.findByNodeTypeAndSourceRecordId(KnowledgeNodeType.TRANSFORMATION, sourceRecordId))
            .thenReturn(Optional.of(focus));
        when(edgeRepository.findByStatus(KnowledgeEdgeStatus.CONFIRMED)).thenReturn(List.of(edge1, edge2));
        when(nodeRepository.findAllById(Mockito.any())).thenAnswer(inv -> List.of(focus, oneHop));
        when(edgeSourceRepository.findByKnowledgeEdgeIdIn(Mockito.any())).thenReturn(List.of());

        var view = service.focusView(KnowledgeNodeType.TRANSFORMATION, sourceRecordId, 1, 25);

        assertEquals(1, view.edges().size());
        assertFalse(view.truncated());
    }

    @Test
    void freshnessReturnsAllCheckpoints() {
        var checkpoint = new KnowledgeProjectionCheckpointEntity(UUID.randomUUID(), "transformations", OffsetDateTime.now());
        when(checkpointRepository.findAll()).thenReturn(List.of(checkpoint));

        var freshness = service.freshness();

        assertEquals(1, freshness.checkpoints().size());
        assertEquals("transformations", freshness.checkpoints().get(0).getSourceModule());
    }
}
