package com.helix.api.knowledge.application;

import com.helix.api.beliefs.adapter.out.persistence.BeliefRepository;
import com.helix.api.beliefs.domain.BeliefEntity;
import com.helix.api.evidence.adapter.out.persistence.EvidenceRepository;
import com.helix.api.evidence.domain.EvidenceDirection;
import com.helix.api.evidence.domain.EvidenceEntity;
import com.helix.api.experiments.adapter.out.persistence.ExperimentRepository;
import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeEdgeRepository;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeEdgeSourceRepository;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeNodeRepository;
import com.helix.api.knowledge.adapter.out.persistence.KnowledgeProjectionCheckpointRepository;
import com.helix.api.knowledge.domain.KnowledgeEdgeConfidence;
import com.helix.api.knowledge.domain.KnowledgeEdgeEntity;
import com.helix.api.knowledge.domain.KnowledgeEdgeOrigin;
import com.helix.api.knowledge.domain.KnowledgeEdgeSourceEntity;
import com.helix.api.knowledge.domain.KnowledgeEdgeStatus;
import com.helix.api.knowledge.domain.KnowledgeEdgeType;
import com.helix.api.knowledge.domain.KnowledgeNodeEntity;
import com.helix.api.knowledge.domain.KnowledgeNodeType;
import com.helix.api.knowledge.domain.KnowledgeProjectionCheckpointEntity;
import com.helix.api.memory.adapter.out.persistence.MemoryProposalRepository;
import com.helix.api.memory.domain.MemoryProposalEntity;
import com.helix.api.memory.domain.MemoryProposalStatus;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Phase 11B (ADR-020): builds the knowledge graph projection from authoritative domain records.
 * Full rebuild only in the first release -- no incremental sync (see
 * docs/product/knowledge-graph-scoping.md Section 12, Q16). Every edge produced here has origin
 * EXPLICIT_DOMAIN_RELATIONSHIP or DETERMINISTIC_DERIVATION and is auto-confirmed; see that
 * document's Section 4 for the exact reasoning behind every edge type below. Nothing in this class
 * writes to any authoritative domain table -- it only reads them and writes to the knowledge_*
 * tables it owns.
 */
@Service
public class KnowledgeGraphProjectionService {

    private static final List<String> SOURCE_MODULES = List.of(
        "transformations", "experiments", "reflections", "evidence", "beliefs", "wisdom", "memory"
    );

    private final TransformationRepository transformationRepository;
    private final ExperimentRepository experimentRepository;
    private final ReflectionRepository reflectionRepository;
    private final EvidenceRepository evidenceRepository;
    private final BeliefRepository beliefRepository;
    private final WisdomEntryRepository wisdomEntryRepository;
    private final WisdomSourceLinkRepository wisdomSourceLinkRepository;
    private final MemoryProposalRepository memoryProposalRepository;
    private final KnowledgeNodeRepository knowledgeNodeRepository;
    private final KnowledgeEdgeRepository knowledgeEdgeRepository;
    private final KnowledgeEdgeSourceRepository knowledgeEdgeSourceRepository;
    private final KnowledgeProjectionCheckpointRepository checkpointRepository;

    public KnowledgeGraphProjectionService(
        TransformationRepository transformationRepository, ExperimentRepository experimentRepository,
        ReflectionRepository reflectionRepository, EvidenceRepository evidenceRepository,
        BeliefRepository beliefRepository, WisdomEntryRepository wisdomEntryRepository,
        WisdomSourceLinkRepository wisdomSourceLinkRepository, MemoryProposalRepository memoryProposalRepository,
        KnowledgeNodeRepository knowledgeNodeRepository, KnowledgeEdgeRepository knowledgeEdgeRepository,
        KnowledgeEdgeSourceRepository knowledgeEdgeSourceRepository,
        KnowledgeProjectionCheckpointRepository checkpointRepository
    ) {
        this.transformationRepository = transformationRepository;
        this.experimentRepository = experimentRepository;
        this.reflectionRepository = reflectionRepository;
        this.evidenceRepository = evidenceRepository;
        this.beliefRepository = beliefRepository;
        this.wisdomEntryRepository = wisdomEntryRepository;
        this.wisdomSourceLinkRepository = wisdomSourceLinkRepository;
        this.memoryProposalRepository = memoryProposalRepository;
        this.knowledgeNodeRepository = knowledgeNodeRepository;
        this.knowledgeEdgeRepository = knowledgeEdgeRepository;
        this.knowledgeEdgeSourceRepository = knowledgeEdgeSourceRepository;
        this.checkpointRepository = checkpointRepository;
    }

    @Transactional
    public RebuildSummary rebuild() {
        var now = OffsetDateTime.now();

        // Full rebuild: wipe and re-derive. Node/edge ids are not stable across rebuilds by design
        // -- (nodeType, sourceRecordId) is the stable key callers should use.
        knowledgeEdgeSourceRepository.deleteAllInBatch();
        knowledgeEdgeRepository.deleteAllInBatch();
        knowledgeNodeRepository.deleteAllInBatch();

        var transformations = transformationRepository.findAll();
        var experiments = experimentRepository.findAll();
        var reflections = reflectionRepository.findAll();
        var evidenceList = evidenceRepository.findAll();
        var beliefs = beliefRepository.findAll();
        var wisdomEntries = wisdomEntryRepository.findAll();
        var wisdomSourceLinks = wisdomSourceLinkRepository.findAll();
        var confirmedMemories = memoryProposalRepository.findAll().stream()
            .filter(m -> m.getStatus() == MemoryProposalStatus.CONFIRMED)
            .toList();

        // Lookups used repeatedly while deriving edges.
        Map<UUID, ExperimentEntity> experimentById = new HashMap<>();
        experiments.forEach(e -> experimentById.put(e.getId(), e));
        Map<UUID, ReflectionEntity> reflectionById = new HashMap<>();
        reflections.forEach(r -> reflectionById.put(r.getId(), r));
        Map<UUID, EvidenceEntity> evidenceById = new HashMap<>();
        evidenceList.forEach(e -> evidenceById.put(e.getId(), e));

        Map<String, KnowledgeNodeEntity> nodeIndex = new HashMap<>(); // key: nodeType + ":" + sourceRecordId
        List<KnowledgeNodeEntity> nodesToSave = new ArrayList<>();
        List<KnowledgeEdgeEntity> edgesToSave = new ArrayList<>();
        List<KnowledgeEdgeSourceEntity> edgeSourcesToSave = new ArrayList<>();

        for (TransformationEntity t : transformations) {
            addNode(nodeIndex, nodesToSave, KnowledgeNodeType.TRANSFORMATION, t.getId(), t.getTitle(),
                t.getPurpose(), null, t.getCreatedAt(), now);
        }
        for (BeliefEntity b : beliefs) {
            addNode(nodeIndex, nodesToSave, KnowledgeNodeType.BELIEF, b.getId(), truncate(b.getStatement()),
                null, b.getType().name(), b.getCreatedAt(), now);
        }
        for (ExperimentEntity e : experiments) {
            addNode(nodeIndex, nodesToSave, KnowledgeNodeType.EXPERIMENT, e.getId(), e.getTitle(),
                e.getHypothesis(), e.getStatus().name(), e.getCreatedAt(), now);
        }
        for (EvidenceEntity e : evidenceList) {
            addNode(nodeIndex, nodesToSave, KnowledgeNodeType.EVIDENCE, e.getId(), truncate(e.getSummary()),
                e.getInterpretation(), e.getDirection().name(), e.getCreatedAt(), now);
        }
        for (ReflectionEntity r : reflections) {
            addNode(nodeIndex, nodesToSave, KnowledgeNodeType.REFLECTION, r.getId(), truncate(r.getContent()),
                null, null, r.getCreatedAt(), now);
        }
        for (WisdomEntryEntity w : wisdomEntries) {
            addNode(nodeIndex, nodesToSave, KnowledgeNodeType.WISDOM, w.getId(), truncate(w.getStatement()),
                null, w.getStatus().name(), w.getCreatedAt(), now);
        }
        for (MemoryProposalEntity m : confirmedMemories) {
            addNode(nodeIndex, nodesToSave, KnowledgeNodeType.MEMORY, m.getId(), truncate(m.getStatement()),
                null, m.getStatus().name(), m.getCreatedAt(), now);
        }

        // --- Category A: explicit domain relationships ---
        for (BeliefEntity b : beliefs) {
            addEdge(edgesToSave, edgeSourcesToSave, nodeIndex,
                KnowledgeNodeType.TRANSFORMATION, b.getTransformationId(), KnowledgeNodeType.BELIEF, b.getId(),
                KnowledgeEdgeType.TRANSFORMATION_CONTAINS_BELIEF, KnowledgeEdgeOrigin.EXPLICIT_DOMAIN_RELATIONSHIP,
                "This belief belongs to this transformation.", now,
                List.of(sourceRef(KnowledgeNodeType.BELIEF, b.getId())));
        }
        for (ExperimentEntity e : experiments) {
            addEdge(edgesToSave, edgeSourcesToSave, nodeIndex,
                KnowledgeNodeType.TRANSFORMATION, e.getTransformationId(), KnowledgeNodeType.EXPERIMENT, e.getId(),
                KnowledgeEdgeType.TRANSFORMATION_CONTAINS_EXPERIMENT, KnowledgeEdgeOrigin.EXPLICIT_DOMAIN_RELATIONSHIP,
                "This experiment was run as part of this transformation.", now,
                List.of(sourceRef(KnowledgeNodeType.EXPERIMENT, e.getId())));
        }
        for (ReflectionEntity r : reflections) {
            addEdge(edgesToSave, edgeSourcesToSave, nodeIndex,
                KnowledgeNodeType.REFLECTION, r.getId(), KnowledgeNodeType.EXPERIMENT, r.getExperimentId(),
                KnowledgeEdgeType.REFLECTION_REFERENCES_EXPERIMENT, KnowledgeEdgeOrigin.EXPLICIT_DOMAIN_RELATIONSHIP,
                "This reflection was recorded for this experiment.", now,
                List.of(sourceRef(KnowledgeNodeType.REFLECTION, r.getId())));

            // Category B: one hop further via the experiment's transformation.
            var experiment = experimentById.get(r.getExperimentId());
            if (experiment != null) {
                addEdge(edgesToSave, edgeSourcesToSave, nodeIndex,
                    KnowledgeNodeType.REFLECTION, r.getId(), KnowledgeNodeType.TRANSFORMATION, experiment.getTransformationId(),
                    KnowledgeEdgeType.REFLECTION_REFERENCES_TRANSFORMATION, KnowledgeEdgeOrigin.DETERMINISTIC_DERIVATION,
                    "This reflection's experiment belongs to this transformation.", now,
                    List.of(sourceRef(KnowledgeNodeType.REFLECTION, r.getId()), sourceRef(KnowledgeNodeType.EXPERIMENT, experiment.getId())));
            }
        }
        for (EvidenceEntity e : evidenceList) {
            var relType = e.getDirection() == EvidenceDirection.SUPPORTS
                ? KnowledgeEdgeType.BELIEF_SUPPORTED_BY_EVIDENCE : KnowledgeEdgeType.BELIEF_CHALLENGED_BY_EVIDENCE;
            var verb = e.getDirection() == EvidenceDirection.SUPPORTS ? "supports" : "challenges";
            addEdge(edgesToSave, edgeSourcesToSave, nodeIndex,
                KnowledgeNodeType.EVIDENCE, e.getId(), KnowledgeNodeType.BELIEF, e.getBeliefId(),
                relType, KnowledgeEdgeOrigin.EXPLICIT_DOMAIN_RELATIONSHIP,
                "This evidence " + verb + " this belief.", now,
                List.of(sourceRef(KnowledgeNodeType.EVIDENCE, e.getId())));

            if (e.getExperimentId() != null) {
                addEdge(edgesToSave, edgeSourcesToSave, nodeIndex,
                    KnowledgeNodeType.EXPERIMENT, e.getExperimentId(), KnowledgeNodeType.EVIDENCE, e.getId(),
                    KnowledgeEdgeType.EXPERIMENT_PRODUCED_EVIDENCE, KnowledgeEdgeOrigin.EXPLICIT_DOMAIN_RELATIONSHIP,
                    "This experiment produced this evidence.", now,
                    List.of(sourceRef(KnowledgeNodeType.EVIDENCE, e.getId())));

                // Category B: same evidence row links both a belief and an experiment.
                addEdge(edgesToSave, edgeSourcesToSave, nodeIndex,
                    KnowledgeNodeType.EXPERIMENT, e.getExperimentId(), KnowledgeNodeType.BELIEF, e.getBeliefId(),
                    KnowledgeEdgeType.BELIEF_EXPLORED_BY_EXPERIMENT, KnowledgeEdgeOrigin.DETERMINISTIC_DERIVATION,
                    "This experiment produced evidence about this belief.", now,
                    List.of(sourceRef(KnowledgeNodeType.EVIDENCE, e.getId())));
            }
            if (e.getReflectionId() != null) {
                addEdge(edgesToSave, edgeSourcesToSave, nodeIndex,
                    KnowledgeNodeType.REFLECTION, e.getReflectionId(), KnowledgeNodeType.EVIDENCE, e.getId(),
                    KnowledgeEdgeType.REFLECTION_PRODUCED_EVIDENCE, KnowledgeEdgeOrigin.EXPLICIT_DOMAIN_RELATIONSHIP,
                    "This reflection produced this evidence.", now,
                    List.of(sourceRef(KnowledgeNodeType.EVIDENCE, e.getId())));
            }
        }
        for (WisdomSourceLinkEntity link : wisdomSourceLinks) {
            if (link.getSourceType() == WisdomSourceType.RETROSPECTIVE) {
                // A weekly retrospective spans every transformation active that week -- not
                // attributable to a single transformation/experiment, so it's excluded from the
                // graph in the first release (see scoping doc Section 4, category B caveat).
                continue;
            }
            if (link.getSourceType() == WisdomSourceType.EVIDENCE) {
                addEdge(edgesToSave, edgeSourcesToSave, nodeIndex,
                    KnowledgeNodeType.EVIDENCE, link.getSourceRecordId(), KnowledgeNodeType.WISDOM, link.getWisdomId(),
                    KnowledgeEdgeType.WISDOM_SUPPORTED_BY_EVIDENCE, KnowledgeEdgeOrigin.EXPLICIT_DOMAIN_RELATIONSHIP,
                    "This wisdom is supported by this evidence.", now,
                    List.of(sourceRef(KnowledgeNodeType.WISDOM, link.getWisdomId())));

                var evidence = evidenceById.get(link.getSourceRecordId());
                deriveWisdomTransformationChain(edgesToSave, edgeSourcesToSave, nodeIndex, experimentById,
                    link.getWisdomId(), evidence != null ? evidence.getExperimentId() : null, link.getWisdomId(), now);
            } else if (link.getSourceType() == WisdomSourceType.REFLECTION) {
                addEdge(edgesToSave, edgeSourcesToSave, nodeIndex,
                    KnowledgeNodeType.REFLECTION, link.getSourceRecordId(), KnowledgeNodeType.WISDOM, link.getWisdomId(),
                    KnowledgeEdgeType.WISDOM_EMERGED_FROM_REFLECTION, KnowledgeEdgeOrigin.EXPLICIT_DOMAIN_RELATIONSHIP,
                    "This wisdom emerged from this reflection.", now,
                    List.of(sourceRef(KnowledgeNodeType.WISDOM, link.getWisdomId())));

                var reflection = reflectionById.get(link.getSourceRecordId());
                deriveWisdomTransformationChain(edgesToSave, edgeSourcesToSave, nodeIndex, experimentById,
                    link.getWisdomId(), reflection != null ? reflection.getExperimentId() : null, link.getWisdomId(), now);
            }
        }
        for (MemoryProposalEntity m : confirmedMemories) {
            var nodeType = mapMemorySourceToNodeType(m.getSourceRecordType());
            if (nodeType == null) continue; // MANUAL_ENTRY / RETROSPECTIVE have no corresponding graph node
            addEdge(edgesToSave, edgeSourcesToSave, nodeIndex,
                nodeType, m.getSourceRecordId(), KnowledgeNodeType.MEMORY, m.getId(),
                KnowledgeEdgeType.MEMORY_DERIVED_FROM, KnowledgeEdgeOrigin.EXPLICIT_DOMAIN_RELATIONSHIP,
                "This memory was derived from this record.", now,
                List.of(sourceRef(KnowledgeNodeType.MEMORY, m.getId())));
        }

        knowledgeNodeRepository.saveAll(nodesToSave);
        knowledgeEdgeRepository.saveAll(edgesToSave);
        knowledgeEdgeSourceRepository.saveAll(edgeSourcesToSave);

        for (String module : SOURCE_MODULES) {
            var checkpoint = checkpointRepository.findBySourceModule(module)
                .orElseGet(() -> new KnowledgeProjectionCheckpointEntity(UUID.randomUUID(), module, now));
            checkpoint.touch(now);
            checkpointRepository.save(checkpoint);
        }

        return new RebuildSummary(nodesToSave.size(), edgesToSave.size(), now);
    }

    /** experimentId -> EXPERIMENT_INFORMED_WISDOM; experiment's transformation -> TRANSFORMATION_PRODUCED_WISDOM. */
    private void deriveWisdomTransformationChain(
        List<KnowledgeEdgeEntity> edgesToSave, List<KnowledgeEdgeSourceEntity> edgeSourcesToSave,
        Map<String, KnowledgeNodeEntity> nodeIndex, Map<UUID, ExperimentEntity> experimentById,
        UUID wisdomId, UUID experimentId, UUID linkRecordId, OffsetDateTime now
    ) {
        if (experimentId == null) return;
        addEdge(edgesToSave, edgeSourcesToSave, nodeIndex,
            KnowledgeNodeType.EXPERIMENT, experimentId, KnowledgeNodeType.WISDOM, wisdomId,
            KnowledgeEdgeType.EXPERIMENT_INFORMED_WISDOM, KnowledgeEdgeOrigin.DETERMINISTIC_DERIVATION,
            "This wisdom traces back to this experiment.", now,
            List.of(sourceRef(KnowledgeNodeType.WISDOM, linkRecordId), sourceRef(KnowledgeNodeType.EXPERIMENT, experimentId)));

        var experiment = experimentById.get(experimentId);
        if (experiment != null) {
            addEdge(edgesToSave, edgeSourcesToSave, nodeIndex,
                KnowledgeNodeType.TRANSFORMATION, experiment.getTransformationId(), KnowledgeNodeType.WISDOM, wisdomId,
                KnowledgeEdgeType.TRANSFORMATION_PRODUCED_WISDOM, KnowledgeEdgeOrigin.DETERMINISTIC_DERIVATION,
                "This wisdom emerged from work on this transformation.", now,
                List.of(sourceRef(KnowledgeNodeType.WISDOM, linkRecordId), sourceRef(KnowledgeNodeType.EXPERIMENT, experimentId)));
        }
    }

    private KnowledgeNodeType mapMemorySourceToNodeType(MemorySourceRecordType type) {
        return switch (type) {
            case REFLECTION -> KnowledgeNodeType.REFLECTION;
            case EXPERIMENT -> KnowledgeNodeType.EXPERIMENT;
            case BELIEF -> KnowledgeNodeType.BELIEF;
            case EVIDENCE -> KnowledgeNodeType.EVIDENCE;
            case WISDOM -> KnowledgeNodeType.WISDOM;
            case MANUAL_ENTRY, RETROSPECTIVE -> null;
        };
    }

    private void addNode(
        Map<String, KnowledgeNodeEntity> nodeIndex, List<KnowledgeNodeEntity> nodesToSave,
        KnowledgeNodeType type, UUID sourceRecordId, String label, String summary, String lifecycleStatus,
        OffsetDateTime createdAt, OffsetDateTime now
    ) {
        var node = new KnowledgeNodeEntity(UUID.randomUUID(), type, sourceRecordId, label, summary,
            lifecycleStatus, createdAt, now);
        nodeIndex.put(key(type, sourceRecordId), node);
        nodesToSave.add(node);
    }

    private void addEdge(
        List<KnowledgeEdgeEntity> edgesToSave, List<KnowledgeEdgeSourceEntity> edgeSourcesToSave,
        Map<String, KnowledgeNodeEntity> nodeIndex,
        KnowledgeNodeType sourceType, UUID sourceRecordId, KnowledgeNodeType targetType, UUID targetRecordId,
        KnowledgeEdgeType relationshipType, KnowledgeEdgeOrigin origin, String explanation, OffsetDateTime now,
        List<SourceRef> sourceRefs
    ) {
        var sourceNode = nodeIndex.get(key(sourceType, sourceRecordId));
        var targetNode = nodeIndex.get(key(targetType, targetRecordId));
        if (sourceNode == null || targetNode == null) return; // referenced record wasn't projected (e.g. filtered out)

        var edge = new KnowledgeEdgeEntity(UUID.randomUUID(), sourceNode.getId(), targetNode.getId(),
            relationshipType, origin, KnowledgeEdgeStatus.CONFIRMED, KnowledgeEdgeConfidence.EXPLICIT, explanation, now);
        edgesToSave.add(edge);
        for (SourceRef ref : sourceRefs) {
            edgeSourcesToSave.add(new KnowledgeEdgeSourceEntity(UUID.randomUUID(), edge.getId(), ref.type(), ref.id()));
        }
    }

    private static SourceRef sourceRef(KnowledgeNodeType type, UUID id) {
        return new SourceRef(type, id);
    }

    private record SourceRef(KnowledgeNodeType type, UUID id) {}

    private static String key(KnowledgeNodeType type, UUID sourceRecordId) {
        return type.name() + ":" + sourceRecordId;
    }

    private static String truncate(String text) {
        if (text == null) return null;
        return text.length() > 280 ? text.substring(0, 280) : text;
    }

    public record RebuildSummary(int nodeCount, int edgeCount, OffsetDateTime rebuiltAt) {}
}
