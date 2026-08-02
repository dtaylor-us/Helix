package com.helix.api.knowledge;

import com.helix.api.beliefs.adapter.out.persistence.BeliefRepository;
import com.helix.api.beliefs.domain.BeliefEntity;
import com.helix.api.beliefs.domain.BeliefType;
import com.helix.api.evidence.adapter.out.persistence.EvidenceRepository;
import com.helix.api.evidence.domain.EvidenceDirection;
import com.helix.api.evidence.domain.EvidenceEntity;
import com.helix.api.evidence.domain.ProvenanceRecordType;
import com.helix.api.evidence.domain.ProvenanceSourceKind;
import com.helix.api.experiments.adapter.out.persistence.ExperimentRepository;
import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.experiments.domain.ExperimentStatus;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeEdgeRepository;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeEdgeSourceRepository;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeNodeRepository;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeProjectionCheckpointRepository;
import com.helix.api.knowledge.application.KnowledgeGraphProjectionService;
import com.helix.api.knowledge.domain.KnowledgeEdgeEntity;
import com.helix.api.knowledge.domain.KnowledgeEdgeType;
import com.helix.api.knowledge.domain.KnowledgeNodeEntity;
import com.helix.api.knowledge.domain.KnowledgeNodeType;
import com.helix.api.knowledge.domain.KnowledgeProjectionCheckpointEntity;
import com.helix.api.memory.adapter.out.persistence.MemoryProposalRepository;
import com.helix.api.memory.domain.MemoryProposalEntity;
import com.helix.api.memory.domain.MemoryProposalStatus;
import com.helix.api.memory.domain.MemorySourceKind;
import com.helix.api.memory.domain.MemorySourceRecordType;
import com.helix.api.reflection.adapter.out.persistence.ReflectionRepository;
import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.transformation.adapter.out.persistence.TransformationRepository;
import com.helix.api.transformation.domain.TransformationEntity;
import com.helix.api.wisdom.adapter.out.persistence.WisdomEntryRepository;
import com.helix.api.wisdom.adapter.out.persistence.WisdomSourceLinkRepository;
import com.helix.api.wisdom.domain.WisdomEntryEntity;
import com.helix.api.wisdom.domain.WisdomSourceLinkEntity;
import com.helix.api.wisdom.domain.WisdomSourceType;
import com.helix.api.wisdom.domain.WisdomStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class KnowledgeGraphProjectionServiceTest {

    private final TransformationRepository transformationRepository = Mockito.mock(TransformationRepository.class);
    private final ExperimentRepository experimentRepository = Mockito.mock(ExperimentRepository.class);
    private final ReflectionRepository reflectionRepository = Mockito.mock(ReflectionRepository.class);
    private final EvidenceRepository evidenceRepository = Mockito.mock(EvidenceRepository.class);
    private final BeliefRepository beliefRepository = Mockito.mock(BeliefRepository.class);
    private final WisdomEntryRepository wisdomEntryRepository = Mockito.mock(WisdomEntryRepository.class);
    private final WisdomSourceLinkRepository wisdomSourceLinkRepository = Mockito.mock(WisdomSourceLinkRepository.class);
    private final MemoryProposalRepository memoryProposalRepository = Mockito.mock(MemoryProposalRepository.class);
    private final KnowledgeNodeRepository knowledgeNodeRepository = Mockito.mock(KnowledgeNodeRepository.class);
    private final KnowledgeEdgeRepository knowledgeEdgeRepository = Mockito.mock(KnowledgeEdgeRepository.class);
    private final KnowledgeEdgeSourceRepository knowledgeEdgeSourceRepository = Mockito.mock(KnowledgeEdgeSourceRepository.class);
    private final KnowledgeProjectionCheckpointRepository checkpointRepository = Mockito.mock(KnowledgeProjectionCheckpointRepository.class);

    private final KnowledgeGraphProjectionService service = new KnowledgeGraphProjectionService(
        transformationRepository, experimentRepository, reflectionRepository, evidenceRepository,
        beliefRepository, wisdomEntryRepository, wisdomSourceLinkRepository, memoryProposalRepository,
        knowledgeNodeRepository, knowledgeEdgeRepository, knowledgeEdgeSourceRepository, checkpointRepository
    );

    @Test
    void rebuildDerivesNodesAndEdgesAcrossTheFullDomainChain() {
        var now = OffsetDateTime.now();
        var transformationId = UUID.randomUUID();
        var beliefId = UUID.randomUUID();
        var experimentId = UUID.randomUUID();
        var reflectionId = UUID.randomUUID();
        var evidenceId = UUID.randomUUID();
        var wisdomId = UUID.randomUUID();
        var memoryId = UUID.randomUUID();

        when(transformationRepository.findAll()).thenReturn(List.of(
            new TransformationEntity(transformationId, "Become steadier under pressure", "Stay calm in conflict", now.minusDays(10))
        ));
        when(beliefRepository.findAll()).thenReturn(List.of(
            new BeliefEntity(beliefId, transformationId, "I fall apart under pressure", BeliefType.LIMITING, now.minusDays(9), now.minusDays(9))
        ));
        when(experimentRepository.findAll()).thenReturn(List.of(
            new ExperimentEntity(experimentId, transformationId, "Pause before responding", "Pausing helps",
                "Breathe once", ExperimentStatus.ACTIVE, now.minusDays(8))
        ));
        when(reflectionRepository.findAll()).thenReturn(List.of(
            new ReflectionEntity(reflectionId, experimentId, "I paused and felt steadier", now.minusDays(7))
        ));
        when(evidenceRepository.findAll()).thenReturn(List.of(
            new EvidenceEntity(evidenceId, beliefId, experimentId, reflectionId, "Stayed calm during a hard conversation",
                "Pausing worked", EvidenceDirection.CHALLENGES, ProvenanceSourceKind.REFLECTION,
                ProvenanceRecordType.REFLECTION, reflectionId, "excerpt", now.minusDays(6))
        ));
        when(wisdomEntryRepository.findAll()).thenReturn(List.of(
            new WisdomEntryEntity(wisdomId, "Pausing before reacting reduces conflict", WisdomStatus.ACCEPTED, null, now.minusDays(5), now.minusDays(5))
        ));
        when(wisdomSourceLinkRepository.findAll()).thenReturn(List.of(
            new WisdomSourceLinkEntity(UUID.randomUUID(), wisdomId, WisdomSourceType.EVIDENCE, evidenceId, "note", now.minusDays(4)),
            new WisdomSourceLinkEntity(UUID.randomUUID(), wisdomId, WisdomSourceType.REFLECTION, reflectionId, "note", now.minusDays(4))
        ));
        when(memoryProposalRepository.findAll()).thenReturn(List.of(
            new MemoryProposalEntity(memoryId, "Pausing helps me stay grounded", MemoryProposalStatus.CONFIRMED,
                MemorySourceKind.REFLECTION, MemorySourceRecordType.REFLECTION, reflectionId, "excerpt", now.minusDays(3), now.minusDays(3)),
            new MemoryProposalEntity(UUID.randomUUID(), "Draft not yet reviewed", MemoryProposalStatus.PROPOSED,
                MemorySourceKind.REFLECTION, MemorySourceRecordType.REFLECTION, reflectionId, "excerpt", now.minusDays(3), now.minusDays(3))
        ));
        when(checkpointRepository.findBySourceModule(any())).thenReturn(Optional.empty());
        when(knowledgeNodeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(knowledgeEdgeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(knowledgeEdgeSourceRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(checkpointRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var summary = service.rebuild();

        // 7 nodes: transformation, belief, experiment, reflection, evidence, wisdom, and only the
        // CONFIRMED memory proposal (the PROPOSED one must be excluded from the projection).
        assertEquals(7, summary.nodeCount());
        assertTrue(summary.edgeCount() > 0);

        Mockito.verify(knowledgeEdgeSourceRepository).deleteAllInBatch();
        Mockito.verify(knowledgeEdgeRepository).deleteAllInBatch();
        Mockito.verify(knowledgeNodeRepository).deleteAllInBatch();

        var edgeCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        Mockito.verify(knowledgeEdgeRepository).saveAll(edgeCaptor.capture());
        @SuppressWarnings("unchecked")
        List<KnowledgeEdgeEntity> savedEdges = edgeCaptor.getValue();
        var edgeTypes = savedEdges.stream().map(KnowledgeEdgeEntity::getRelationshipType).toList();

        assertTrue(edgeTypes.contains(KnowledgeEdgeType.TRANSFORMATION_CONTAINS_BELIEF));
        assertTrue(edgeTypes.contains(KnowledgeEdgeType.TRANSFORMATION_CONTAINS_EXPERIMENT));
        assertTrue(edgeTypes.contains(KnowledgeEdgeType.REFLECTION_REFERENCES_EXPERIMENT));
        assertTrue(edgeTypes.contains(KnowledgeEdgeType.REFLECTION_REFERENCES_TRANSFORMATION));
        assertTrue(edgeTypes.contains(KnowledgeEdgeType.BELIEF_CHALLENGED_BY_EVIDENCE));
        assertTrue(edgeTypes.contains(KnowledgeEdgeType.EXPERIMENT_PRODUCED_EVIDENCE));
        assertTrue(edgeTypes.contains(KnowledgeEdgeType.BELIEF_EXPLORED_BY_EXPERIMENT));
        assertTrue(edgeTypes.contains(KnowledgeEdgeType.REFLECTION_PRODUCED_EVIDENCE));
        assertTrue(edgeTypes.contains(KnowledgeEdgeType.WISDOM_SUPPORTED_BY_EVIDENCE));
        assertTrue(edgeTypes.contains(KnowledgeEdgeType.WISDOM_EMERGED_FROM_REFLECTION));
        assertTrue(edgeTypes.contains(KnowledgeEdgeType.EXPERIMENT_INFORMED_WISDOM));
        assertTrue(edgeTypes.contains(KnowledgeEdgeType.TRANSFORMATION_PRODUCED_WISDOM));
        assertTrue(edgeTypes.contains(KnowledgeEdgeType.MEMORY_DERIVED_FROM));

        var nodeCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        Mockito.verify(knowledgeNodeRepository).saveAll(nodeCaptor.capture());
        @SuppressWarnings("unchecked")
        List<KnowledgeNodeEntity> savedNodes = nodeCaptor.getValue();
        assertEquals(1, savedNodes.stream().filter(n -> n.getNodeType() == KnowledgeNodeType.MEMORY).count());

        // Direction matters: the frontend renders "{source label} {displayLabel} {target label}" as
        // a plain-English sentence, so a swapped source/target produces a directionally false claim
        // even though the relationship type itself is "present." Assert (source type, target type)
        // for every edge type, not just that the type exists somewhere in the saved list.
        var nodeTypeById = savedNodes.stream().collect(java.util.stream.Collectors.toMap(KnowledgeNodeEntity::getId, KnowledgeNodeEntity::getNodeType));
        for (KnowledgeEdgeEntity edge : savedEdges) {
            var sourceType = nodeTypeById.get(edge.getSourceNodeId());
            var targetType = nodeTypeById.get(edge.getTargetNodeId());
            switch (edge.getRelationshipType()) {
                case TRANSFORMATION_CONTAINS_BELIEF -> assertEdgeDirection(KnowledgeNodeType.TRANSFORMATION, KnowledgeNodeType.BELIEF, sourceType, targetType);
                case TRANSFORMATION_CONTAINS_EXPERIMENT -> assertEdgeDirection(KnowledgeNodeType.TRANSFORMATION, KnowledgeNodeType.EXPERIMENT, sourceType, targetType);
                case TRANSFORMATION_PRODUCED_WISDOM -> assertEdgeDirection(KnowledgeNodeType.TRANSFORMATION, KnowledgeNodeType.WISDOM, sourceType, targetType);
                case BELIEF_SUPPORTED_BY_EVIDENCE, BELIEF_CHALLENGED_BY_EVIDENCE -> assertEdgeDirection(KnowledgeNodeType.BELIEF, KnowledgeNodeType.EVIDENCE, sourceType, targetType);
                case BELIEF_EXPLORED_BY_EXPERIMENT -> assertEdgeDirection(KnowledgeNodeType.BELIEF, KnowledgeNodeType.EXPERIMENT, sourceType, targetType);
                case EXPERIMENT_PRODUCED_EVIDENCE -> assertEdgeDirection(KnowledgeNodeType.EXPERIMENT, KnowledgeNodeType.EVIDENCE, sourceType, targetType);
                case EXPERIMENT_INFORMED_WISDOM -> assertEdgeDirection(KnowledgeNodeType.EXPERIMENT, KnowledgeNodeType.WISDOM, sourceType, targetType);
                case REFLECTION_PRODUCED_EVIDENCE -> assertEdgeDirection(KnowledgeNodeType.REFLECTION, KnowledgeNodeType.EVIDENCE, sourceType, targetType);
                case REFLECTION_REFERENCES_EXPERIMENT -> assertEdgeDirection(KnowledgeNodeType.REFLECTION, KnowledgeNodeType.EXPERIMENT, sourceType, targetType);
                case REFLECTION_REFERENCES_TRANSFORMATION -> assertEdgeDirection(KnowledgeNodeType.REFLECTION, KnowledgeNodeType.TRANSFORMATION, sourceType, targetType);
                case WISDOM_SUPPORTED_BY_EVIDENCE -> assertEdgeDirection(KnowledgeNodeType.WISDOM, KnowledgeNodeType.EVIDENCE, sourceType, targetType);
                case WISDOM_EMERGED_FROM_REFLECTION -> assertEdgeDirection(KnowledgeNodeType.WISDOM, KnowledgeNodeType.REFLECTION, sourceType, targetType);
                case MEMORY_DERIVED_FROM -> assertEquals(KnowledgeNodeType.MEMORY, sourceType, "MEMORY_DERIVED_FROM must have MEMORY as source");
                case BELIEF_RELATED_TO_BELIEF -> { /* symmetric -- no fixed direction to assert */ }
            }
        }

        Mockito.verify(checkpointRepository, Mockito.times(7)).save(any());
    }

    private static void assertEdgeDirection(KnowledgeNodeType expectedSource, KnowledgeNodeType expectedTarget, KnowledgeNodeType actualSource, KnowledgeNodeType actualTarget) {
        assertEquals(expectedSource, actualSource, "expected source " + expectedSource + " but was " + actualSource);
        assertEquals(expectedTarget, actualTarget, "expected target " + expectedTarget + " but was " + actualTarget);
    }

    @Test
    void rebuildExcludesRetrospectiveSourcedWisdomLinksFromTheGraph() {
        var now = OffsetDateTime.now();
        var wisdomId = UUID.randomUUID();
        var retrospectiveId = UUID.randomUUID();

        when(transformationRepository.findAll()).thenReturn(List.of());
        when(beliefRepository.findAll()).thenReturn(List.of());
        when(experimentRepository.findAll()).thenReturn(List.of());
        when(reflectionRepository.findAll()).thenReturn(List.of());
        when(evidenceRepository.findAll()).thenReturn(List.of());
        when(wisdomEntryRepository.findAll()).thenReturn(List.of(
            new WisdomEntryEntity(wisdomId, "A retrospective-derived lesson", WisdomStatus.ACCEPTED, retrospectiveId, now, now)
        ));
        when(wisdomSourceLinkRepository.findAll()).thenReturn(List.of(
            new WisdomSourceLinkEntity(UUID.randomUUID(), wisdomId, WisdomSourceType.RETROSPECTIVE, retrospectiveId, "note", now)
        ));
        when(memoryProposalRepository.findAll()).thenReturn(List.of());
        when(checkpointRepository.findBySourceModule(any())).thenReturn(Optional.empty());
        when(knowledgeNodeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(knowledgeEdgeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(knowledgeEdgeSourceRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(checkpointRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var summary = service.rebuild();

        assertEquals(1, summary.nodeCount());
        assertEquals(0, summary.edgeCount());
    }

    @Test
    void rebuildSkipsEdgesWhoseEndpointNodeWasNotProjected() {
        // Belief references a transformation id that was never returned by the transformation
        // repository (e.g. deleted or filtered) -- the edge must be silently skipped, not fail.
        var now = OffsetDateTime.now();
        var missingTransformationId = UUID.randomUUID();
        var beliefId = UUID.randomUUID();

        when(transformationRepository.findAll()).thenReturn(List.of());
        when(beliefRepository.findAll()).thenReturn(List.of(
            new BeliefEntity(beliefId, missingTransformationId, "Orphaned belief", BeliefType.EMPOWERING, now, now)
        ));
        when(experimentRepository.findAll()).thenReturn(List.of());
        when(reflectionRepository.findAll()).thenReturn(List.of());
        when(evidenceRepository.findAll()).thenReturn(List.of());
        when(wisdomEntryRepository.findAll()).thenReturn(List.of());
        when(wisdomSourceLinkRepository.findAll()).thenReturn(List.of());
        when(memoryProposalRepository.findAll()).thenReturn(List.of());
        when(checkpointRepository.findBySourceModule(any())).thenReturn(Optional.empty());
        when(knowledgeNodeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(knowledgeEdgeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(knowledgeEdgeSourceRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(checkpointRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var summary = service.rebuild();

        assertEquals(1, summary.nodeCount());
        assertEquals(0, summary.edgeCount());
    }

    @Test
    void rebuildTouchesACheckpointForEverySourceModule() {
        when(transformationRepository.findAll()).thenReturn(List.of());
        when(beliefRepository.findAll()).thenReturn(List.of());
        when(experimentRepository.findAll()).thenReturn(List.of());
        when(reflectionRepository.findAll()).thenReturn(List.of());
        when(evidenceRepository.findAll()).thenReturn(List.of());
        when(wisdomEntryRepository.findAll()).thenReturn(List.of());
        when(wisdomSourceLinkRepository.findAll()).thenReturn(List.of());
        when(memoryProposalRepository.findAll()).thenReturn(List.of());
        var existing = new KnowledgeProjectionCheckpointEntity(UUID.randomUUID(), "transformations", OffsetDateTime.now().minusDays(1));
        when(checkpointRepository.findBySourceModule("transformations")).thenReturn(Optional.of(existing));
        when(checkpointRepository.findBySourceModule(Mockito.argThat(m -> !"transformations".equals(m)))).thenReturn(Optional.empty());
        when(knowledgeNodeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(knowledgeEdgeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(knowledgeEdgeSourceRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(checkpointRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.rebuild();

        Mockito.verify(checkpointRepository, Mockito.times(7)).save(any());
        assertFalse(existing.getLastProjectedAt().isBefore(OffsetDateTime.now().minusMinutes(1)));
    }
}
