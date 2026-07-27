package com.helix.api.experiments.adapter.in.http;

import com.helix.api.experiments.application.ExperimentService;
import com.helix.api.experiments.domain.ExperimentEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
public class ExperimentController {

    private final ExperimentService service;

    public ExperimentController(ExperimentService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/transformations/{transformationId}/experiments")
    public ExperimentDto create(
        @PathVariable UUID transformationId,
        @Valid @RequestBody CreateExperimentRequest request
    ) {
        return toDto(service.create(
            transformationId,
            request.title(),
            request.hypothesis(),
            request.nextAction(),
            request.cadence(),
            request.evidenceOfSuccess(),
            request.reviewAt()
        ));
    }

    @GetMapping("/api/v1/experiments/{id}")
    public ExperimentDto get(@PathVariable UUID id) {
        return toDto(service.get(id));
    }

    private ExperimentDto toDto(ExperimentEntity entity) {
        return new ExperimentDto(
            entity.getId(),
            entity.getTransformationId(),
            entity.getTitle(),
            entity.getHypothesis(),
            entity.getNextAction(),
            entity.getCadence(),
            entity.getEvidenceOfSuccess(),
            entity.getReviewAt() != null ? entity.getReviewAt().toString() : null,
            entity.getStatus().name(),
            entity.getCreatedAt().toString()
        );
    }

    public record CreateExperimentRequest(
        @NotBlank @Size(max = 180) String title,
        @Size(max = 2000) String hypothesis,
        @Size(max = 500) String nextAction,
        @Size(max = 200) String cadence,
        @Size(max = 2000) String evidenceOfSuccess,
        LocalDate reviewAt
    ) {}

    public record ExperimentDto(
        UUID id,
        UUID transformationId,
        String title,
        String hypothesis,
        String nextAction,
        String cadence,
        String evidenceOfSuccess,
        String reviewAt,
        String status,
        String createdAt
    ) {}
}
