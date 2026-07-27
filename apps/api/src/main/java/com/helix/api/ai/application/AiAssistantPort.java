package com.helix.api.ai.application;

public interface AiAssistantPort {
    AiSuggestion suggestReflectiveQuestion(String context);

    record AiSuggestion(String text, String provider, String model, String promptVersion, boolean deterministicFallback) {}
}
