package com.helix.api.evidence.application;

import com.helix.api.beliefs.application.BeliefService;
import com.helix.api.evidence.adapter.out.persistence.EvidenceRepository;
import com.helix.api.evidence.domain.EvidenceDirection;
import com.helix.api.evidence.domain.EvidenceEntity;
import com.helix.api.evidence.domain.ProvenanceRecordType;
import com.helix.api.evidence.domain.ProvenanceSourceKind;
import com.helix.api.experiments.application.ExperimentService;
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

    public EvidenceService(EvidenceRepository repository, BeliefService beliefService,
                           ExperimentService experimentService, ReflectionService reflectionService) {
        this.repository = repository;
        this.beliefService = beliefService;
        this.experimentService = experimentService;
        this.reflectionService = reflectionService;
    }

    @Transactional
    public EvidenceEntity create(UUID beliefId, UUID experimentId, UUID reflectionId, String summary,
                                 String interpretation, EvidenceDirection direction,
                                 ProvenanceSourceKind sourceKind, ProvenanceRecordType recordType,
                                 UUID recordId, String excerpt) {
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
            OffsetDateTime.now()
        );
        return repository.save(evidence);
    }

    public List<EvidenceEntity> timeline(UUID beliefId) {
        beliefService.get(beliefId);
        return repository.findByBeliefIdOrderByCreatedAtDesc(beliefId);
    }

    public EvidenceEntity get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Evidence not found"));
    }

    public List<EvidenceEntity> search(String query) {
        var normalized = query.trim();
        return repository.findTop20BySummaryContainingIgnoreCaseOrInterpretationContainingIgnoreCaseOrderByCreatedAtDesc(
            normalized,
            normalized
        );
    }
}