package com.helix.api.knowledge.application;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeEdgeRepository;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeEdgeSourceRepository;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeNodeRepository;
import com.helix.api.knowledge.domain.KnowledgeEdgeConfidence;
import com.helix.api.knowledge.domain.KnowledgeEdgeEntity;
import com.helix.api.knowledge.domain.KnowledgeEdgeOrigin;
import com.helix.api.knowledge.domain.KnowledgeEdgeSourceEntity;
import com.helix.api.knowledge.domain.KnowledgeEdgeStatus;
import com.helix.api.knowledge.domain.KnowledgeEdgeType;
import com.helix.api.knowledge.domain.KnowledgeNodeEntity;
import com.helix.api.knowledge.domain.KnowledgeNodeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Phase 11E (ADR-020): the only part of the knowledge graph that ever asks AI to judge a
 * relationship rather than deriving one deterministically. Compares pairs of BELIEF nodes that
 * have no existing BELIEF_RELATED_TO_BELIEF edge between them (of any status -- a previously
 * rejected pair is not re-asked) and asks the AI port whether they're thematically connected.
 * Anything the AI says "yes" to lands as KnowledgeEdgeStatus.PROPOSED / KnowledgeEdgeOrigin
 * .AI_PROPOSED, never CONFIRMED -- a human reviews it through the Phase 11D governance actions
 * before it's treated as trustworthy (ADR-008). This is a manually triggered pass, not something
 * that runs automatically on every projection rebuild, to keep AI usage visible and bounded.
 */
@Service
public class KnowledgeGraphRelationshipDiscoveryService {

    /** Caps AI calls per invocation so this stays a bounded, on-demand action, not an unbounded scan. */
    public static final int MAX_PAIRS_PER_RUN = 25;

    private final KnowledgeNodeRepository nodeRepository;
    private final KnowledgeEdgeRepository edgeRepository;
    private final KnowledgeEdgeSourceRepository edgeSourceRepository;
    private final AiAssistantPort aiAssistantPort;
    private final CurrentUserProvider currentUserProvider;

    public KnowledgeGraphRelationshipDiscoveryService(
        KnowledgeNodeRepository nodeRepository, KnowledgeEdgeRepository edgeRepository,
        KnowledgeEdgeSourceRepository edgeSourceRepository, AiAssistantPort aiAssistantPort,
        CurrentUserProvider currentUserProvider
    ) {
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.edgeSourceRepository = edgeSourceRepository;
        this.aiAssistantPort = aiAssistantPort;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public DiscoveryRunSummary discoverBeliefRelationships() {
        var ownerId = currentUserProvider.currentUserId();
        var beliefNodes = nodeRepository.findByOwnerIdAndNodeType(ownerId, KnowledgeNodeType.BELIEF);
        var alreadyConnected = existingBeliefRelationPairs(ownerId);

        int pairsEvaluated = 0;
        int proposalsCreated = 0;

        outer:
        for (int i = 0; i < beliefNodes.size(); i++) {
            for (int j = i + 1; j < beliefNodes.size(); j++) {
                if (pairsEvaluated >= MAX_PAIRS_PER_RUN) {
                    break outer;
                }

                var nodeA = beliefNodes.get(i);
                var nodeB = beliefNodes.get(j);
                if (alreadyConnected.contains(pairKey(nodeA.getId(), nodeB.getId()))) {
                    continue;
                }

                pairsEvaluated++;
                var proposal = aiAssistantPort.proposeBeliefRelationship(buildContext(nodeA, nodeB));
                if (!proposal.related()) {
                    continue;
                }

                var now = OffsetDateTime.now();
                var explanation = proposal.explanation() != null && !proposal.explanation().isBlank()
                    ? proposal.explanation()
                    : "AI noticed a thematic connection between these beliefs.";
                var edge = new KnowledgeEdgeEntity(
                    UUID.randomUUID(), nodeA.getId(), nodeB.getId(), KnowledgeEdgeType.BELIEF_RELATED_TO_BELIEF,
                    KnowledgeEdgeOrigin.AI_PROPOSED, KnowledgeEdgeStatus.PROPOSED, KnowledgeEdgeConfidence.MODERATE,
                    explanation, now, ownerId
                );
                edgeRepository.save(edge);
                edgeSourceRepository.save(new KnowledgeEdgeSourceEntity(UUID.randomUUID(), edge.getId(), KnowledgeNodeType.BELIEF, nodeA.getSourceRecordId(), ownerId));
                edgeSourceRepository.save(new KnowledgeEdgeSourceEntity(UUID.randomUUID(), edge.getId(), KnowledgeNodeType.BELIEF, nodeB.getSourceRecordId(), ownerId));
                proposalsCreated++;
            }
        }

        return new DiscoveryRunSummary(pairsEvaluated, proposalsCreated);
    }

    private Set<String> existingBeliefRelationPairs(UUID ownerId) {
        Set<String> pairs = new HashSet<>();
        for (KnowledgeEdgeEntity edge : edgeRepository.findByOwnerIdAndRelationshipType(ownerId, KnowledgeEdgeType.BELIEF_RELATED_TO_BELIEF)) {
            pairs.add(pairKey(edge.getSourceNodeId(), edge.getTargetNodeId()));
        }
        return pairs;
    }

    private static String pairKey(UUID a, UUID b) {
        // Order-independent: BELIEF_RELATED_TO_BELIEF has no meaningful direction.
        return a.compareTo(b) <= 0 ? a + ":" + b : b + ":" + a;
    }

    private String buildContext(KnowledgeNodeEntity nodeA, KnowledgeNodeEntity nodeB) {
        return "Belief A: " + nodeA.getDisplayLabel() + "\nBelief B: " + nodeB.getDisplayLabel();
    }

    public record DiscoveryRunSummary(int pairsEvaluated, int proposalsCreated) {}
}
