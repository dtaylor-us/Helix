package com.helix.api.knowledge.adapter.in.http;

import com.helix.api.knowledge.application.KnowledgeEdgeGovernanceService;
import com.helix.api.knowledge.application.KnowledgeGraphProjectionService;
import com.helix.api.knowledge.application.KnowledgeGraphQueryService;
import com.helix.api.knowledge.domain.KnowledgeEdgeEntity;
import com.helix.api.knowledge.domain.KnowledgeEdgeSourceEntity;
import com.helix.api.knowledge.domain.KnowledgeEdgeType;
import com.helix.api.knowledge.domain.KnowledgeNodeEntity;
import com.helix.api.knowledge.domain.KnowledgeNodeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 11B/C/D (ADR-020). Every response is a bounded, focus-node-centered view (never the whole
 * graph) plus a plain-language explanation on every edge. See docs/product/knowledge-graph-scoping.md.
 */
@RestController
@RequestMapping("/api/v1/knowledge-graph")
public class KnowledgeGraphController {

    private static final Map<KnowledgeEdgeType, String> DISPLAY_LABELS = new EnumMap<>(KnowledgeEdgeType.class);
    static {
        DISPLAY_LABELS.put(KnowledgeEdgeType.TRANSFORMATION_CONTAINS_BELIEF, "Includes belief");
        DISPLAY_LABELS.put(KnowledgeEdgeType.TRANSFORMATION_CONTAINS_EXPERIMENT, "Explored through");
        DISPLAY_LABELS.put(KnowledgeEdgeType.TRANSFORMATION_PRODUCED_WISDOM, "Produced lesson");
        DISPLAY_LABELS.put(KnowledgeEdgeType.BELIEF_SUPPORTED_BY_EVIDENCE, "Supported by");
        DISPLAY_LABELS.put(KnowledgeEdgeType.BELIEF_CHALLENGED_BY_EVIDENCE, "Challenged by");
        DISPLAY_LABELS.put(KnowledgeEdgeType.BELIEF_EXPLORED_BY_EXPERIMENT, "Tested through");
        DISPLAY_LABELS.put(KnowledgeEdgeType.EXPERIMENT_PRODUCED_EVIDENCE, "Produced");
        DISPLAY_LABELS.put(KnowledgeEdgeType.EXPERIMENT_INFORMED_WISDOM, "Informed");
        DISPLAY_LABELS.put(KnowledgeEdgeType.REFLECTION_PRODUCED_EVIDENCE, "Produced");
        DISPLAY_LABELS.put(KnowledgeEdgeType.REFLECTION_REFERENCES_EXPERIMENT, "Recorded for");
        DISPLAY_LABELS.put(KnowledgeEdgeType.REFLECTION_REFERENCES_TRANSFORMATION, "Relates to");
        DISPLAY_LABELS.put(KnowledgeEdgeType.WISDOM_SUPPORTED_BY_EVIDENCE, "Supported by");
        DISPLAY_LABELS.put(KnowledgeEdgeType.WISDOM_EMERGED_FROM_REFLECTION, "Emerged from");
        DISPLAY_LABELS.put(KnowledgeEdgeType.MEMORY_DERIVED_FROM, "Derived from");
    }

    private final KnowledgeGraphProjectionService projectionService;
    private final KnowledgeGraphQueryService queryService;
    private final KnowledgeEdgeGovernanceService governanceService;

    public KnowledgeGraphController(
        KnowledgeGraphProjectionService projectionService, KnowledgeGraphQueryService queryService,
        KnowledgeEdgeGovernanceService governanceService
    ) {
        this.projectionService = projectionService;
        this.queryService = queryService;
        this.governanceService = governanceService;
    }

    @PostMapping("/rebuild")
    public RebuildResponseDto rebuild() {
        var summary = projectionService.rebuild();
        return new RebuildResponseDto(summary.nodeCount(), summary.edgeCount(), summary.rebuiltAt().toString());
    }

    @GetMapping("/status")
    public StatusResponseDto status() {
        var freshness = queryService.freshness();
        var checkpoints = freshness.checkpoints().stream()
            .map(c -> new CheckpointDto(c.getSourceModule(), c.getLastProjectedAt().toString()))
            .toList();
        return new StatusResponseDto(checkpoints);
    }

    @GetMapping("/transformation/{transformationId}")
    public GraphViewDto transformationView(@PathVariable UUID transformationId) {
        return toDto(queryService.focusView(KnowledgeNodeType.TRANSFORMATION, transformationId),
            "Connections for this transformation",
            "Beliefs, experiments, evidence, and wisdom connected to this transformation.");
    }

    @GetMapping("/belief/{beliefId}")
    public GraphViewDto beliefView(@PathVariable UUID beliefId) {
        return toDto(queryService.focusView(KnowledgeNodeType.BELIEF, beliefId),
            "Connections for this belief",
            "Experiments, evidence, and wisdom connected to this belief.");
    }

    @GetMapping("/focus/{nodeType}/{sourceRecordId}")
    public GraphViewDto focusView(@PathVariable KnowledgeNodeType nodeType, @PathVariable UUID sourceRecordId) {
        return toDto(queryService.focusView(nodeType, sourceRecordId), "Connections", "Records connected to this one.");
    }

    @PostMapping("/edges/{edgeId}/confirm")
    public GraphEdgeDto confirmEdge(@PathVariable UUID edgeId) {
        return toEdgeDto(governanceService.confirm(edgeId), Map.of());
    }

    @PostMapping("/edges/{edgeId}/reject")
    public GraphEdgeDto rejectEdge(@PathVariable UUID edgeId) {
        return toEdgeDto(governanceService.reject(edgeId), Map.of());
    }

    @PostMapping("/edges/{edgeId}/hide")
    public GraphEdgeDto hideEdge(@PathVariable UUID edgeId) {
        return toEdgeDto(governanceService.hide(edgeId), Map.of());
    }

    private GraphViewDto toDto(KnowledgeGraphQueryService.GraphView view, String title, String description) {
        var nodes = view.nodes().stream().map(this::toNodeDto).toList();
        var edges = view.edges().stream()
            .map(e -> toEdgeDto(e, view.edgeSourcesByEdge()))
            .toList();
        return new GraphViewDto(title, description, view.focusNode().getId(), nodes, edges, view.truncated());
    }

    private GraphNodeDto toNodeDto(KnowledgeNodeEntity node) {
        return new GraphNodeDto(
            node.getId(), node.getNodeType().name(), node.getDisplayLabel(), node.getSummary(),
            node.getSourceRecordId(), sourceRoute(node.getNodeType(), node.getSourceRecordId()),
            node.getLifecycleStatus(), node.getNodeType().name().toLowerCase()
        );
    }

    private GraphEdgeDto toEdgeDto(KnowledgeEdgeEntity edge, Map<UUID, List<KnowledgeEdgeSourceEntity>> edgeSourcesByEdge) {
        var sourceRefs = edgeSourcesByEdge.getOrDefault(edge.getId(), List.of()).stream()
            .map(s -> new SourceReferenceDto(s.getRecordType().name(), s.getRecordId()))
            .toList();
        return new GraphEdgeDto(
            edge.getId(), edge.getSourceNodeId(), edge.getTargetNodeId(), edge.getRelationshipType().name(),
            DISPLAY_LABELS.getOrDefault(edge.getRelationshipType(), edge.getRelationshipType().name()),
            edge.getOrigin().name(), edge.getStatus().name(), edge.getConfidence().name(),
            edge.getExplanation(), sourceRefs
        );
    }

    private String sourceRoute(KnowledgeNodeType type, UUID sourceRecordId) {
        return switch (type) {
            case TRANSFORMATION -> "/transformations/" + sourceRecordId;
            case EXPERIMENT -> "/experiments/" + sourceRecordId;
            case REFLECTION -> "/reflections/" + sourceRecordId;
            case BELIEF, EVIDENCE -> "/knowledge";
            case WISDOM -> "/wisdom";
            case MEMORY -> "/settings/memory";
        };
    }

    public record RebuildResponseDto(int nodeCount, int edgeCount, String rebuiltAt) {}
    public record CheckpointDto(String sourceModule, String lastProjectedAt) {}
    public record StatusResponseDto(List<CheckpointDto> checkpoints) {}
    public record GraphViewDto(String title, String description, UUID focusNodeId,
                               List<GraphNodeDto> nodes, List<GraphEdgeDto> edges, boolean truncated) {}
    public record GraphNodeDto(UUID id, String type, String label, String summary, UUID sourceRecordId,
                               String sourceRoute, String status, String visualCategory) {}
    public record GraphEdgeDto(UUID id, UUID sourceNodeId, UUID targetNodeId, String relationshipType,
                               String displayLabel, String origin, String status, String confidence,
                               String explanation, List<SourceReferenceDto> sourceReferences) {}
    public record SourceReferenceDto(String recordType, UUID recordId) {}
}
