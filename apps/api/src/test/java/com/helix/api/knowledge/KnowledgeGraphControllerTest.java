package com.helix.api.knowledge;

import com.helix.api.knowledge.adapter.in.http.KnowledgeGraphController;
import com.helix.api.knowledge.application.KnowledgeEdgeGovernanceService;
import com.helix.api.knowledge.application.KnowledgeGraphProjectionService;
import com.helix.api.knowledge.application.KnowledgeGraphQueryService;
import com.helix.api.knowledge.application.KnowledgeGraphRelationshipDiscoveryService;
import com.helix.api.knowledge.domain.KnowledgeEdgeConfidence;
import com.helix.api.knowledge.domain.KnowledgeEdgeEntity;
import com.helix.api.knowledge.domain.KnowledgeEdgeOrigin;
import com.helix.api.knowledge.domain.KnowledgeEdgeStatus;
import com.helix.api.knowledge.domain.KnowledgeEdgeType;
import com.helix.api.knowledge.domain.KnowledgeNodeEntity;
import com.helix.api.knowledge.domain.KnowledgeNodeType;
import com.helix.api.shared.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KnowledgeGraphControllerTest {

    private final KnowledgeGraphProjectionService projectionService = Mockito.mock(KnowledgeGraphProjectionService.class);
    private final KnowledgeGraphQueryService queryService = Mockito.mock(KnowledgeGraphQueryService.class);
    private final KnowledgeEdgeGovernanceService governanceService = Mockito.mock(KnowledgeEdgeGovernanceService.class);
    private final KnowledgeGraphRelationshipDiscoveryService discoveryService = Mockito.mock(KnowledgeGraphRelationshipDiscoveryService.class);
    private final MockMvc mockMvc = MockMvcBuilders
        .standaloneSetup(new KnowledgeGraphController(projectionService, queryService, governanceService, discoveryService))
        .setControllerAdvice(new ApiExceptionHandler())
        .build();

    private KnowledgeNodeEntity node(KnowledgeNodeType type, UUID sourceRecordId, String label) {
        return new KnowledgeNodeEntity(UUID.randomUUID(), type, sourceRecordId, label, "summary", "ACTIVE",
            OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    void rebuildReturnsNodeAndEdgeCounts() throws Exception {
        Mockito.when(projectionService.rebuild()).thenReturn(
            new KnowledgeGraphProjectionService.RebuildSummary(12, 20, OffsetDateTime.now())
        );

        mockMvc.perform(post("/api/v1/knowledge-graph/rebuild"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nodeCount").value(12))
            .andExpect(jsonPath("$.edgeCount").value(20));
    }

    @Test
    void transformationViewReturnsBoundedGraphWithPlainLanguageEdgeLabels() throws Exception {
        var transformationId = UUID.randomUUID();
        var focus = node(KnowledgeNodeType.TRANSFORMATION, transformationId, "Become steadier under pressure");
        var beliefSourceId = UUID.randomUUID();
        var belief = node(KnowledgeNodeType.BELIEF, beliefSourceId, "I fall apart under pressure");
        var edge = new KnowledgeEdgeEntity(UUID.randomUUID(), focus.getId(), belief.getId(),
            KnowledgeEdgeType.TRANSFORMATION_CONTAINS_BELIEF, KnowledgeEdgeOrigin.EXPLICIT_DOMAIN_RELATIONSHIP,
            KnowledgeEdgeStatus.CONFIRMED, KnowledgeEdgeConfidence.EXPLICIT, "This belief belongs to this transformation.",
            OffsetDateTime.now());

        Mockito.when(queryService.focusView(KnowledgeNodeType.TRANSFORMATION, transformationId)).thenReturn(
            new KnowledgeGraphQueryService.GraphView(focus, List.of(focus, belief), List.of(edge), Map.of(), false)
        );

        mockMvc.perform(get("/api/v1/knowledge-graph/transformation/" + transformationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.focusNodeId").value(focus.getId().toString()))
            .andExpect(jsonPath("$.nodes.length()").value(2))
            .andExpect(jsonPath("$.edges[0].displayLabel").value("Includes belief"))
            .andExpect(jsonPath("$.edges[0].explanation").value("This belief belongs to this transformation."))
            .andExpect(jsonPath("$.truncated").value(false))
            .andExpect(jsonPath("$.nodes[0].sourceRoute").value("/transformations/" + transformationId))
            .andExpect(jsonPath("$.edges[0].history.createdAt").value(edge.getCreatedAt().toString()))
            .andExpect(jsonPath("$.edges[0].history.confirmedAt").value(edge.getConfirmedAt().toString()));
    }

    @Test
    void focusViewReturnsNotFoundWithActionableMessageWhenProjectionIsStale() throws Exception {
        var missingId = UUID.randomUUID();
        Mockito.when(queryService.focusView(KnowledgeNodeType.BELIEF, missingId))
            .thenThrow(new NoSuchElementException("No knowledge graph node found -- rebuild the projection."));

        mockMvc.perform(get("/api/v1/knowledge-graph/belief/" + missingId))
            .andExpect(status().isNotFound());
    }

    @Test
    void confirmEdgeDelegatesToGovernanceServiceAndReturnsUpdatedEdge() throws Exception {
        var edgeId = UUID.randomUUID();
        var confirmedEdge = new KnowledgeEdgeEntity(edgeId, UUID.randomUUID(), UUID.randomUUID(),
            KnowledgeEdgeType.WISDOM_SUPPORTED_BY_EVIDENCE, KnowledgeEdgeOrigin.AI_PROPOSED,
            KnowledgeEdgeStatus.PROPOSED, KnowledgeEdgeConfidence.MODERATE, "explanation", OffsetDateTime.now());
        confirmedEdge.confirm(OffsetDateTime.now());
        Mockito.when(governanceService.confirm(edgeId)).thenReturn(confirmedEdge);

        mockMvc.perform(post("/api/v1/knowledge-graph/edges/" + edgeId + "/confirm"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.displayLabel").value("Supported by"));
    }

    @Test
    void discoverRelationshipsReturnsPairAndProposalCounts() throws Exception {
        Mockito.when(discoveryService.discoverBeliefRelationships()).thenReturn(
            new KnowledgeGraphRelationshipDiscoveryService.DiscoveryRunSummary(6, 2)
        );

        mockMvc.perform(post("/api/v1/knowledge-graph/discover-relationships"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pairsEvaluated").value(6))
            .andExpect(jsonPath("$.proposalsCreated").value(2));
    }

    @Test
    void statusReturnsCheckpointFreshness() throws Exception {
        Mockito.when(queryService.freshness()).thenReturn(
            new KnowledgeGraphQueryService.ProjectionFreshness(List.of())
        );

        mockMvc.perform(get("/api/v1/knowledge-graph/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.checkpoints.length()").value(0));
    }
}
