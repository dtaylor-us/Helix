package com.helix.api.knowledge.application;

import com.helix.api.knowledge.adapter.out.persistence.KnowledgeEdgeRepository;
import com.helix.api.knowledge.domain.KnowledgeEdgeEntity;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Phase 11D: governance actions over proposed edges. In the first release every edge ships
 * pre-confirmed (Phase 11B/C), so these actions have nothing to act on until Phase 11E starts
 * producing AI_PROPOSED edges -- the mechanism ships now so 11E doesn't also need to build this.
 * Per ADR-019/ADR-008's established pattern: this never writes to any authoritative domain record,
 * only to the graph projection's own edge status.
 */
@Service
public class KnowledgeEdgeGovernanceService {

    private final KnowledgeEdgeRepository edgeRepository;

    public KnowledgeEdgeGovernanceService(KnowledgeEdgeRepository edgeRepository) {
        this.edgeRepository = edgeRepository;
    }

    public KnowledgeEdgeEntity confirm(UUID edgeId) {
        var edge = get(edgeId);
        edge.confirm(OffsetDateTime.now());
        return edgeRepository.save(edge);
    }

    public KnowledgeEdgeEntity reject(UUID edgeId) {
        var edge = get(edgeId);
        edge.reject(OffsetDateTime.now());
        return edgeRepository.save(edge);
    }

    public KnowledgeEdgeEntity hide(UUID edgeId) {
        var edge = get(edgeId);
        edge.hide();
        return edgeRepository.save(edge);
    }

    private KnowledgeEdgeEntity get(UUID edgeId) {
        return edgeRepository.findById(edgeId)
            .orElseThrow(() -> new NoSuchElementException("Knowledge graph edge not found"));
    }
}
