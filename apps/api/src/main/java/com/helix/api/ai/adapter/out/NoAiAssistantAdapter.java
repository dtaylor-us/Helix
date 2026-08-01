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
    public AiWeeklySummary summarizeWeek(String context) {
        return new AiWeeklySummary(
            "This week's reflections are recorded below.",
            "Choose one recurring pattern and run a smaller experiment next week.",
            "none",
            "deterministic",
            true
        );
    }

    @Override
    public AiExperimentDraft proposeExperiment(String context) {
        return new AiExperimentDraft(
            "Try one small step this week",
            null,
            "Spend five minutes today on the smallest version of this.",
            null,
            null,
            "none",
            "deterministic",
            true
        );
    }

    @Override
    public AiSuggestion continueReflectionChat(String context) {
        return new AiSuggestion(
            "What else stood out about today?",
            "none",
            "deterministic",
            "v1",
            true
        );
    }

    @Override
    public AiReflectionStructure structureReflection(String context) {
        return new AiReflectionStructure(
            "I reflected on what happened today and noticed a few meaningful moments.",
            null,
            null,
            null,
            null,
            "none",
            "deterministic",
            true
        );
    }
}
