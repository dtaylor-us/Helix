package com.helix.api.reflection.adapter.in.http;

import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.suggestions.domain.SuggestionEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class ReflectionController {

    private final ReflectionService service;

    public ReflectionController(ReflectionService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/experiments/{experimentId}/reflections")
    public CreateReflectionResponse create(
        @PathVariable UUID experimentId,
        @Valid @RequestBody CreateReflectionRequest request
    ) {
        var result = service.create(experimentId, request.content());
        return new CreateReflectionResponse(toReflectionDto(result.reflection()), toSuggestionDto(result.suggestion()));
    }

    @GetMapping("/api/v1/reflections/{id}")
    public ReflectionDto get(@PathVariable UUID id) {
        return toReflectionDto(service.get(id));
    }

    private ReflectionDto toReflectionDto(ReflectionEntity entity) {
        return new ReflectionDto(entity.getId(), entity.getExperimentId(), entity.getContent(), entity.getCreatedAt().toString());
    }

    private SuggestionDto toSuggestionDto(SuggestionEntity entity) {
        return new SuggestionDto(
            entity.getId(),
            entity.getExperimentId(),
            entity.getReflectionId(),
            entity.getText(),
            entity.getStatus().name(),
            entity.getReplacementText(),
            entity.getCreatedAt().toString(),
            entity.getRespondedAt() != null ? entity.getRespondedAt().toString() : null
        );
    }

    public record CreateReflectionRequest(@NotBlank @Size(max = 4000) String content) {}
    public record CreateReflectionResponse(ReflectionDto reflection, SuggestionDto suggestion) {}

    public record ReflectionDto(UUID id, UUID experimentId, String content, String createdAt) {}

    public record SuggestionDto(
        UUID id,
        UUID experimentId,
        UUID reflectionId,
        String text,
        String status,
        String replacementText,
        String createdAt,
        String respondedAt
    ) {}
}
