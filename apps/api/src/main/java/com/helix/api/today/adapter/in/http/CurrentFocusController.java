package com.helix.api.today.adapter.in.http;

import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.suggestions.domain.SuggestionEntity;
import com.helix.api.today.application.CurrentFocusService;
import com.helix.api.transformation.domain.TransformationEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Phase 7 projection endpoint. Returns everything Today's UI needs in one response: onboarding
 * progress, the user's transformations, and (if any) the active experiment with its reflection and
 * suggestion history — replacing two separate client calls to {@code /today} and
 * {@code /transformations}.
 */
@RestController
public class CurrentFocusController {

    private final CurrentFocusService service;

    public CurrentFocusController(CurrentFocusService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/current-focus")
    public CurrentFocusDto currentFocus() {
        var snapshot = service.snapshot();
        var transformations = snapshot.transformations().stream().map(this::toTransformationDto).toList();

        if (snapshot.today() == null) {
            return new CurrentFocusDto(
                snapshot.onboardingStatus().name(), transformations, false, null, List.of(), List.of()
            );
        }

        var today = snapshot.today();
        return new CurrentFocusDto(
            snapshot.onboardingStatus().name(),
            transformations,
            true,
            new ExperimentCard(today.activeExperiment().getId(), today.activeExperiment().getTransformationId(),
                today.activeExperiment().getTitle(), today.activeExperiment().getHypothesis(),
                today.activeExperiment().getNextAction(), today.activeExperiment().getCadence(),
                today.activeExperiment().getEvidenceOfSuccess(),
                today.activeExperiment().getReviewAt() != null ? today.activeExperiment().getReviewAt().toString() : null,
                today.activeExperiment().getStatus().name(),
                today.activeExperiment().getCreatedAt().toString()),
            today.reflectionHistory().stream().map(this::toReflectionCard).toList(),
            today.suggestionHistory().stream().map(this::toSuggestionCard).toList()
        );
    }

    private TransformationDto toTransformationDto(TransformationEntity entity) {
        return new TransformationDto(
            entity.getId(), entity.getTitle(), entity.getPurpose(), entity.getDesiredIdentity(),
            entity.getObstacle(), entity.getCreatedAt().toString()
        );
    }

    private ReflectionCard toReflectionCard(ReflectionEntity entity) {
        return new ReflectionCard(
            entity.getId(), entity.getExperimentId(), entity.getContent(), entity.getAttempted(),
            entity.getNoticed(), entity.getEvidenceNoted(), entity.getSurprise(), entity.getCreatedAt().toString()
        );
    }

    private SuggestionCard toSuggestionCard(SuggestionEntity entity) {
        return new SuggestionCard(
            entity.getId(), entity.getExperimentId(), entity.getReflectionId(), entity.getText(),
            entity.getStatus().name(), entity.getReplacementText(), entity.getCreatedAt().toString(),
            entity.getRespondedAt() != null ? entity.getRespondedAt().toString() : null,
            entity.getSource().name(), entity.getAiProvider(), entity.getAiModel()
        );
    }

    public record CurrentFocusDto(String onboardingStatus, List<TransformationDto> transformations,
                                  boolean hasActiveExperiment, ExperimentCard activeExperiment,
                                  List<ReflectionCard> reflectionHistory, List<SuggestionCard> suggestionHistory) {}
    public record TransformationDto(UUID id, String title, String purpose, String desiredIdentity,
                                    String obstacle, String createdAt) {}
    public record ExperimentCard(UUID id, UUID transformationId, String title, String hypothesis,
                                 String nextAction, String cadence, String evidenceOfSuccess, String reviewAt,
                                 String status, String createdAt) {}
    public record ReflectionCard(UUID id, UUID experimentId, String content, Boolean attempted,
                                 String noticed, String evidenceNoted, String surprise, String createdAt) {}
    public record SuggestionCard(UUID id, UUID experimentId, UUID reflectionId, String text,
                                 String status, String replacementText, String createdAt, String respondedAt,
                                 String source, String aiProvider, String aiModel) {}
}
