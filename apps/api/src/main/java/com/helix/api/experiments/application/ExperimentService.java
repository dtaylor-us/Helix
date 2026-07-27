package com.helix.api.experiments.application;

import com.helix.api.experiments.adapter.out.persistence.ExperimentRepository;
import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.experiments.domain.ExperimentStatus;
import com.helix.api.transformation.application.TransformationService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExperimentService {

    private final ExperimentRepository repository;
    private final TransformationService transformationService;

    public ExperimentService(ExperimentRepository repository, TransformationService transformationService) {
        this.repository = repository;
        this.transformationService = transformationService;
    }

    public ExperimentEntity create(UUID transformationId, String title, String hypothesis, String nextAction) {
        transformationService.get(transformationId);
        var entity = new ExperimentEntity(
            UUID.randomUUID(),
            transformationId,
            title.trim(),
            hypothesis,
            nextAction,
            ExperimentStatus.ACTIVE,
            OffsetDateTime.now()
        );
        return repository.save(entity);
    }

    public ExperimentEntity get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Experiment not found"));
    }

    public Optional<ExperimentEntity> activeExperiment() {
        return repository.findFirstByStatusOrderByCreatedAtDesc(ExperimentStatus.ACTIVE);
    }
}
