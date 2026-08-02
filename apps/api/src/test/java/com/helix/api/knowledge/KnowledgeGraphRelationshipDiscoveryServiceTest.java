package com.helix.api.knowledge;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeEdgeRepository;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeEdgeSourceRepository;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeNodeRepository;
import com.helix.api.knowledge.application.KnowledgeGraphRelationshipDiscoveryService;
import com.helix.api.knowledge.domain.KnowledgeEdgeConfidence;
import com.helix.api.knowledge.domain.KnowledgeEdgeEntity;
import com.helix.api.knowledge.domain.KnowledgeEdgeOrigin;
import com.helix.api.knowledge.domain.KnowledgeEdgeStatus;
import com.helix.api.knowledge.domain.KnowledgeEdgeType;
import com.helix.api.knowledge.domain.KnowledgeNodeEntity;
import com.helix.api.knowledge.domain.KnowledgeNodeType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class KnowledgeGraphRelationshipDiscoveryServiceTest {

    private final KnowledgeNodeRepository nodeRepository = Mockito.mock(KnowledgeNodeRepository.class);
    private final KnowledgeEdgeRepository edgeRepository = Mockito.mock(KnowledgeEdgeRepository.class);
    private final KnowledgeEdgeSourceRepository edgeSourceRepository = Mockito.mock(KnowledgeEdgeSourceRepository.class);
    private final AiAssistantPort aiAssistantPort = Mockito.mock(AiAssistantPort.class);

    private final KnowledgeGraphRelationshipDiscoveryService service = new KnowledgeGraphRelationshipDiscoveryService(
        nodeRepository, edgeRepository, edgeSourceRepository, aiAssistantPort
    );

    private KnowledgeNodeEntity beliefNode(String label) {
        return new KnowledgeNodeEntity(UUID.randomUUID(), KnowledgeNodeType.BELIEF, UUID.randomUUID(), label, null,
            "LIMITING", OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void createsAProposedAiEdgeWhenTheModelSaysTheBeliefsAreRelated() {
        var beliefA = beliefNode("I fall apart under pressure");
        var beliefB = beliefNode("I avoid conflict at work");
        when(nodeRepository.findByNodeType(KnowledgeNodeType.BELIEF)).thenReturn(List.of(beliefA, beliefB));
        when(edgeRepository.findByRelationshipType(KnowledgeEdgeType.BELIEF_RELATED_TO_BELIEF)).thenReturn(List.of());
        when(aiAssistantPort.proposeBeliefRelationship(any())).thenReturn(
            new AiAssistantPort.AiRelationshipProposal(true, "Both stem from fear of losing control.", "openai", "gpt-4o-mini", false)
        );

        var summary = service.discoverBeliefRelationships();

        assertEquals(1, summary.pairsEvaluated());
        assertEquals(1, summary.proposalsCreated());

        var edgeCaptor = ArgumentCaptor.forClass(KnowledgeEdgeEntity.class);
        Mockito.verify(edgeRepository).save(edgeCaptor.capture());
        var savedEdge = edgeCaptor.getValue();
        assertEquals(KnowledgeEdgeType.BELIEF_RELATED_TO_BELIEF, savedEdge.getRelationshipType());
        assertEquals(KnowledgeEdgeOrigin.AI_PROPOSED, savedEdge.getOrigin());
        assertEquals(KnowledgeEdgeStatus.PROPOSED, savedEdge.getStatus());
        assertEquals(KnowledgeEdgeConfidence.MODERATE, savedEdge.getConfidence());
        assertEquals("Both stem from fear of losing control.", savedEdge.getExplanation());
        // Never auto-confirmed -- a human must review it via the Phase 11D governance actions.
        assertNull(savedEdge.getConfirmedAt());

        Mockito.verify(edgeSourceRepository, Mockito.times(2)).save(any());
    }

    @Test
    void doesNotCreateAnEdgeWhenTheModelSaysTheyAreNotRelated() {
        var beliefA = beliefNode("I fall apart under pressure");
        var beliefB = beliefNode("I love quiet mornings");
        when(nodeRepository.findByNodeType(KnowledgeNodeType.BELIEF)).thenReturn(List.of(beliefA, beliefB));
        when(edgeRepository.findByRelationshipType(KnowledgeEdgeType.BELIEF_RELATED_TO_BELIEF)).thenReturn(List.of());
        when(aiAssistantPort.proposeBeliefRelationship(any())).thenReturn(
            new AiAssistantPort.AiRelationshipProposal(false, null, "openai", "gpt-4o-mini", false)
        );

        var summary = service.discoverBeliefRelationships();

        assertEquals(1, summary.pairsEvaluated());
        assertEquals(0, summary.proposalsCreated());
        Mockito.verify(edgeRepository, Mockito.never()).save(any());
    }

    @Test
    void skipsPairsThatAlreadyHaveABeliefRelatedToBeliefEdgeRegardlessOfStatus() {
        var beliefA = beliefNode("I fall apart under pressure");
        var beliefB = beliefNode("I avoid conflict at work");
        when(nodeRepository.findByNodeType(KnowledgeNodeType.BELIEF)).thenReturn(List.of(beliefA, beliefB));

        // A previously rejected proposal between this exact pair -- must not be re-asked.
        var existingEdge = new KnowledgeEdgeEntity(UUID.randomUUID(), beliefA.getId(), beliefB.getId(),
            KnowledgeEdgeType.BELIEF_RELATED_TO_BELIEF, KnowledgeEdgeOrigin.AI_PROPOSED, KnowledgeEdgeStatus.REJECTED,
            KnowledgeEdgeConfidence.MODERATE, "explanation", OffsetDateTime.now());
        when(edgeRepository.findByRelationshipType(KnowledgeEdgeType.BELIEF_RELATED_TO_BELIEF)).thenReturn(List.of(existingEdge));

        var summary = service.discoverBeliefRelationships();

        assertEquals(0, summary.pairsEvaluated());
        assertEquals(0, summary.proposalsCreated());
        Mockito.verifyNoInteractions(aiAssistantPort);
    }

    @Test
    void capsTheNumberOfPairsEvaluatedPerRun() {
        // MAX_PAIRS_PER_RUN = 25; 8 beliefs = 28 pairs, so this must stop at 25, not run all 28.
        var beliefs = new java.util.ArrayList<KnowledgeNodeEntity>();
        for (int i = 0; i < 8; i++) {
            beliefs.add(beliefNode("Belief " + i));
        }
        when(nodeRepository.findByNodeType(KnowledgeNodeType.BELIEF)).thenReturn(beliefs);
        when(edgeRepository.findByRelationshipType(KnowledgeEdgeType.BELIEF_RELATED_TO_BELIEF)).thenReturn(List.of());
        when(aiAssistantPort.proposeBeliefRelationship(any())).thenReturn(
            new AiAssistantPort.AiRelationshipProposal(false, null, "openai", "gpt-4o-mini", false)
        );

        var summary = service.discoverBeliefRelationships();

        assertEquals(KnowledgeGraphRelationshipDiscoveryService.MAX_PAIRS_PER_RUN, summary.pairsEvaluated());
        assertTrue(28 > KnowledgeGraphRelationshipDiscoveryService.MAX_PAIRS_PER_RUN);
    }
}
