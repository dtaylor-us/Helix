package com.helix.api.experiments.adapter.in.http;

import com.helix.api.experiments.application.ExperimentService;
import com.helix.api.experiments.domain.ExperimentEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

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
        return toDto(service.create(transformationId, request.title(), request.hypothesis(), request.nextAction()));
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
            entity.getStatus().name(),
            entity.getCreatedAt().toString()
        );
    }

    public record CreateExperimentRequest(
        @NotBlank @Size(max = 180) String title,
        @Size(max = 2000) String hypothesis,
        @Size(max = 500) String nextAction
    ) {}

    public record ExperimentDto(
        UUID id,
        UUID transformationId,
        String title,
        String hypothesis,
        String nextAction,
        String status,
        String createdAt
    ) {}
}
