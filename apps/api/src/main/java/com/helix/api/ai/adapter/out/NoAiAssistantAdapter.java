package com.helix.api.ai.adapter.out;

import com.helix.api.ai.application.AiAssistantPort;
import org.springframework.stereotype.Component;

@Component
public class NoAiAssistantAdapter implements AiAssistantPort {

    @Override
    public AiSuggestion suggestReflectiveQuestion(String context) {
        return new AiSuggestion(
            "What felt lighter or heavier after today’s experiment?",
            "none",
            "deterministic",
            "v1",
            true
        );
    }

    @Override
    public AiSuggestion suggestNextAction(String context) {
        return new AiSuggestion(
            "Try repeating today's experiment on a smaller scale tomorrow.",
            "none",
            "deterministic",
            "v1",
            true
        );
    }

    @Override
    public AiExperimentDraft proposeExperiment(ExperimentDraftRequest request) {
        String transformationTitle = hasText(request.transformationTitle()) ? request.transformationTitle().trim() : "this transformation";
        return new AiExperimentDraft(
            trimToLength("First small step toward " + transformationTitle, 180),
            "A smaller, repeatable action will help me learn what actually moves this transformation forward.",
            "Choose one action you can finish in under ten minutes and try it once today.",
            "Once today",
            "You notice one concrete sign that this transformation felt easier to practice.",
            "none",
            "deterministic",
            "v1",
            true
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToLength(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
