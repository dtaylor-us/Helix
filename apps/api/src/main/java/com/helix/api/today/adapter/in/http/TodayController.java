package com.helix.api.today.adapter.in.http;

import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.suggestions.domain.SuggestionEntity;
import com.helix.api.today.application.TodayService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class TodayController {

    private final TodayService service;

    public TodayController(TodayService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/today")
    public TodayDto today() {
        return service.snapshot()
            .map(snapshot -> new TodayDto(
                true,
                new ExperimentCard(snapshot.activeExperiment().getId(), snapshot.activeExperiment().getTransformationId(),
                    snapshot.activeExperiment().getTitle(), snapshot.activeExperiment().getHypothesis(),
                    snapshot.activeExperiment().getNextAction(), snapshot.activeExperiment().getCadence(),
                    snapshot.activeExperiment().getEvidenceOfSuccess(),
                    snapshot.activeExperiment().getReviewAt() != null ? snapshot.activeExperiment().getReviewAt().toString() : null,
                    snapshot.activeExperiment().getStatus().name(),
                    snapshot.activeExperiment().getCreatedAt().toString()),
                snapshot.reflectionHistory().stream().map(this::toReflectionCard).toList(),
                snapshot.suggestionHistory().stream().map(this::toSuggestionCard).toList()
            ))
            .orElse(new TodayDto(false, null, List.of(), List.of()));
    }

    private ReflectionCard toReflectionCard(ReflectionEntity entity) {
        return new ReflectionCard(entity.getId(), entity.getExperimentId(), entity.getContent(), entity.getCreatedAt().toString());
    }

    private SuggestionCard toSuggestionCard(SuggestionEntity entity) {
        return new SuggestionCard(
            entity.getId(), entity.getExperimentId(), entity.getReflectionId(), entity.getText(),
            entity.getStatus().name(), entity.getReplacementText(), entity.getCreatedAt().toString(),
            entity.getRespondedAt() != null ? entity.getRespondedAt().toString() : null
        );
    }

    public record TodayDto(boolean hasActiveExperiment, ExperimentCard activeExperiment,
                           List<ReflectionCard> reflectionHistory, List<SuggestionCard> suggestionHistory) {}
    public record ExperimentCard(UUID id, UUID transformationId, String title, String hypothesis,
                                 String nextAction, String cadence, String evidenceOfSuccess, String reviewAt,
                                 String status, String createdAt) {}
    public record ReflectionCard(UUID id, UUID experimentId, String content, String createdAt) {}
    public record SuggestionCard(UUID id, UUID experimentId, UUID reflectionId, String text,
                                 String status, String replacementText, String createdAt, String respondedAt) {}
}
