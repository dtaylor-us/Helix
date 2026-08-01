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
}
