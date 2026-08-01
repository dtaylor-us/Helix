package com.helix.api.ai.application;

public interface AiAssistantPort {
    AiSuggestion suggestReflectiveQuestion(String context);

    /**
     * Propose a single small next action based on a reflection (and its surrounding experiment
     * context). Distinct from {@link #suggestReflectiveQuestion(String)}, which asks a question
     * rather than proposing an action. See ADR-016: this is a required (not optional) content
     * source for the "Suggested Small Action" feature — the returned {@code deterministicFallback}
     * flag distinguishes a live model answer from an outage/no-provider fallback.
     */
    AiSuggestion suggestNextAction(String context);

    record AiSuggestion(String text, String provider, String model, String promptVersion, boolean deterministicFallback) {}
}
