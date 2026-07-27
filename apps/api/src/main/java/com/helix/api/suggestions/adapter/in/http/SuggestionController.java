package com.helix.api.suggestions.adapter.in.http;

import com.helix.api.suggestions.application.SuggestionService;
import com.helix.api.suggestions.domain.SuggestionEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suggestions")
public class SuggestionController {

    private final SuggestionService service;

    public SuggestionController(SuggestionService service) {
        this.service = service;
    }

    @PostMapping("/{id}/accept")
    public SuggestionDto accept(@PathVariable UUID id) {
        return toDto(service.accept(id));
    }

    @PostMapping("/{id}/dismiss")
    public SuggestionDto dismiss(@PathVariable UUID id) {
        return toDto(service.dismiss(id));
    }

    @PostMapping("/{id}/replace")
    public SuggestionDto replace(@PathVariable UUID id, @Valid @RequestBody ReplaceSuggestionRequest request) {
        return toDto(service.replace(id, request.replacementText()));
    }

    private SuggestionDto toDto(SuggestionEntity entity) {
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

    public record ReplaceSuggestionRequest(@NotBlank @Size(max = 500) String replacementText) {}
    public record SuggestionDto(UUID id, UUID experimentId, UUID reflectionId, String text, String status,
                                String replacementText, String createdAt, String respondedAt) {}
}
