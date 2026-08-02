package com.helix.api.knowledge.application;

import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeEdgeRepository;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeEdgeSourceRepository;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeNodeRepository;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeProjectionCheckpointRepository;
import com.helix.api.knowledge.domain.KnowledgeEdgeEntity;
import com.helix.api.knowledge.domain.KnowledgeEdgeSourceEntity;
import com.helix.api.knowledge.domain.KnowledgeEdgeStatus;
import com.helix.api.knowledge.domain.KnowledgeNodeEntity;
import com.helix.api.knowledge.domain.KnowledgeNodeType;
import com.helix.api.knowledge.domain.KnowledgeProjectionCheckpointEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * Phase 11B (ADR-020): bounded, focus-node-centered graph views. Never returns the whole graph --
 * every query is a breadth-first walk from one focus node, capped by depth and node count (see
 * docs/product/knowledge-graph-scoping.md Section 11/12). Only CONFIRMED edges are included; the
 * first release has no PROPOSED edges to include or exclude (Phase 11E).
 */
@Service
public class KnowledgeGraphQueryService {

    public static final int DEFAULT_MAX_NODES = 25;
    public static final int DEFAULT_MAX_DEPTH = 2;

    private final KnowledgeNodeRepository nodeRepository;
    private final KnowledgeEdgeRepository edgeRepository;
    private final KnowledgeEdgeSourceRepository edgeSourceRepository;
    private final KnowledgeProjectionCheckpointRepository checkpointRepository;
    private final CurrentUserProvider currentUserProvider;

    public KnowledgeGraphQueryService(
        KnowledgeNodeRepository nodeRepository, KnowledgeEdgeRepository edgeRepository,
        KnowledgeEdgeSourceRepository edgeSourceRepository, KnowledgeProjectionCheckpointRepository checkpointRepository,
        CurrentUserProvider currentUserProvider
    ) {
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.edgeSourceRepository = edgeSourceRepository;
        this.checkpointRepository = checkpointRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public GraphView focusView(KnowledgeNodeType nodeType, UUID sourceRecordId) {
        return focusView(nodeType, sourceRecordId, DEFAULT_MAX_DEPTH, DEFAULT_MAX_NODES);
    }

    public GraphView focusView(KnowledgeNodeType nodeType, UUID sourceRecordId, int maxDepth, int maxNodes) {
        var ownerId = currentUserProvider.currentUserId();
        var focusNode = nodeRepository.findByOwnerIdAndNodeTypeAndSourceRecordId(ownerId, nodeType, sourceRecordId)
            .orElseThrow(() -> new NoSuchElementException(
                "No knowledge graph node found for " + nodeType + " " + sourceRecordId
                    + " -- the projection may need to be rebuilt (POST /api/v1/knowledge-graph/rebuild)."));

        var confirmedEdges = edgeRepository.findByOwnerIdAndStatus(ownerId, KnowledgeEdgeStatus.CONFIRMED);
        Map<UUID, List<KnowledgeEdgeEntity>> edgesByNode = new HashMap<>();
        for (var edge : confirmedEdges) {
            edgesByNode.computeIfAbsent(edge.getSourceNodeId(), k -> new ArrayList<>()).add(edge);
            edgesByNode.computeIfAbsent(edge.getTargetNodeId(), k -> new ArrayList<>()).add(edge);
        }

        Set<UUID> visitedNodeIds = new HashSet<>();
        visitedNodeIds.add(focusNode.getId());
        List<KnowledgeEdgeEntity> includedEdges = new ArrayList<>();
        boolean truncated = false;

        var frontier = new ArrayDeque<UUID>();
        frontier.add(focusNode.getId());
        int depth = 0;
        while (!frontier.isEmpty() && depth < maxDepth) {
            var next = new ArrayDeque<UUID>();
            while (!frontier.isEmpty()) {
                var currentId = frontier.poll();
                for (var edge : edgesByNode.getOrDefault(currentId, List.of())) {
                    var neighborId = edge.getSourceNodeId().equals(currentId) ? edge.getTargetNodeId() : edge.getSourceNodeId();
                    if (!visitedNodeIds.contains(neighborId)) {
                        if (visitedNodeIds.size() >= maxNodes) {
                            truncated = true;
                            continue;
                        }
                        visitedNodeIds.add(neighborId);
                        next.add(neighborId);
                    }
                    includedEdges.add(edge);
                }
            }
            frontier = next;
            depth++;
        }

        var nodes = nodeRepository.findByOwnerIdAndIdIn(ownerId, visitedNodeIds);
        var edgeIds = includedEdges.stream().map(KnowledgeEdgeEntity::getId).distinct().toList();
        var edgeSourcesByEdge = edgeSourceRepository.findByOwnerIdAndKnowledgeEdgeIdIn(ownerId, edgeIds).stream()
            .collect(java.util.stream.Collectors.groupingBy(KnowledgeEdgeSourceEntity::getKnowledgeEdgeId));

        var distinctEdges = includedEdges.stream()
            .filter(distinctById(KnowledgeEdgeEntity::getId))
            .sorted(Comparator.comparing(KnowledgeEdgeEntity::getCreatedAt))
            .toList();

        return new GraphView(focusNode, nodes, distinctEdges, edgeSourcesByEdge, truncated);
    }

    public ProjectionFreshness freshness() {
        return new ProjectionFreshness(checkpointRepository.findAllByOwnerId(currentUserProvider.currentUserId()));
    }

    private static <T> java.util.function.Predicate<T> distinctById(java.util.function.Function<T, UUID> idExtractor) {
        Set<UUID> seen = new HashSet<>();
        return item -> seen.add(idExtractor.apply(item));
    }

    public record GraphView(
        KnowledgeNodeEntity focusNode,
        List<KnowledgeNodeEntity> nodes,
        List<KnowledgeEdgeEntity> edges,
        Map<UUID, List<KnowledgeEdgeSourceEntity>> edgeSourcesByEdge,
        boolean truncated
    ) {}

    public record ProjectionFreshness(List<KnowledgeProjectionCheckpointEntity> checkpoints) {}
}
