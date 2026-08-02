package com.helix.api.knowledge.application;

import com.helix.api.evidence.adapter.out.persistence.EvidenceRepository;
import com.helix.api.knowledge.domain.KnowledgeNodeType;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Resolves knowledge graph nodes to their user-facing record routes. */
@Service
public class KnowledgeSourceRouteService {

    private final EvidenceRepository evidenceRepository;

    public KnowledgeSourceRouteService(EvidenceRepository evidenceRepository) {
        this.evidenceRepository = evidenceRepository;
    }

    public String sourceRoute(KnowledgeNodeType type, UUID sourceRecordId) {
        return switch (type) {
            case TRANSFORMATION -> "/transformations/" + sourceRecordId;
            case EXPERIMENT -> "/experiments/" + sourceRecordId;
            case REFLECTION -> "/reflections/" + sourceRecordId;
            case BELIEF -> "/knowledge?beliefId=" + sourceRecordId;
            case EVIDENCE -> evidenceRepository.findById(sourceRecordId)
                .map(evidence -> "/knowledge?beliefId=" + evidence.getBeliefId())
                .orElse("/knowledge");
            case WISDOM -> "/wisdom";
            case MEMORY -> "/settings/memory";
        };
    }
}
