package com.helix.api.evidence.application;

import com.helix.api.beliefs.application.BeliefService;
import com.helix.api.evidence.adapter.out.persistence.EvidenceRepository;
import com.helix.api.evidence.domain.EvidenceDirection;
import com.helix.api.evidence.domain.EvidenceEntity;
import com.helix.api.evidence.domain.ProvenanceRecordType;
import com.helix.api.evidence.domain.ProvenanceSourceKind;
import com.helix.api.experiments.application.ExperimentService;
import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.reflection.application.ReflectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class EvidenceService {

    private final EvidenceRepository repository;
    private final BeliefService beliefService;
    private final ExperimentService experimentService;
    private final ReflectionService reflectionService;
    private final CurrentUserProvider currentUserProvider;

    public EvidenceService(EvidenceRepository repository, BeliefService beliefService,
                           ExperimentService experimentService, ReflectionService reflectionService,
                           CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.beliefService = beliefService;
        this.experimentService = experimentService;
        this.reflectionService = reflectionService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public EvidenceEntity create(UUID beliefId, UUID experimentId, UUID reflectionId, String summary,
                                 String interpretation, EvidenceDirection direction,
                                 ProvenanceSourceKind sourceKind, ProvenanceRecordType recordType,
                                 UUID recordId, String excerpt) {
        // Each of these 404s if the referenced id doesn't belong to the caller.
        beliefService.get(beliefId);
        if (experimentId != null) {
            experimentService.get(experimentId);
        }
        if (reflectionId != null) {
            reflectionService.get(reflectionId);
        }
        var evidence = new EvidenceEntity(
            UUID.randomUUID(),
            beliefId,
            experimentId,
            reflectionId,
            summary.trim(),
            interpretation == null || interpretation.isBlank() ? null : interpretation.trim(),
            direction,
            sourceKind,
            recordType,
            recordId,
            excerpt == null || excerpt.isBlank() ? null : excerpt.trim(),
            OffsetDateTime.now(),
            currentUserProvider.currentUserId()
        );
        return repository.save(evidence);
    }

    public List<EvidenceEntity> timeline(UUID beliefId) {
        // 404s if beliefId doesn't belong to the caller.
        beliefService.get(beliefId);
        return repository.findByBeliefIdOrderByCreatedAtDesc(beliefId);
    }

    public EvidenceEntity get(UUID id) {
        var evidence = repository.findById(id).orElseThrow(() -> new NoSuchElementException("Evidence not found"));
        if (!currentUserProvider.currentUserId().equals(evidence.getOwnerId())) {
            throw new NoSuchElementException("Evidence not found");
        }
        return evidence;
    }

    public List<EvidenceEntity> search(String query) {
        var normalized = query.trim();
        var ownerId = currentUserProvider.currentUserId();
        return repository.findTop20ByOwnerIdAndSummaryContainingIgnoreCaseOrOwnerIdAndInterpretationContainingIgnoreCaseOrderByCreatedAtDesc(
            ownerId, normalized, ownerId, normalized
        );
    }
}