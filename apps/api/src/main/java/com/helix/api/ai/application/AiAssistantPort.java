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

    /**
     * Produce a narrative weekly retrospective (a short summary of the week's reflections plus a
     * specific "what to try next" suggestion) from a context string built out of that week's
     * reflection excerpts. See ADR-016 (Phase 5 slice B): required content source for this feature,
     * same fallback-on-failure convention as {@link #suggestNextAction(String)}.
     */
    AiWeeklySummary summarizeWeek(String context);

    /**
     * Draft a proposed experiment (title/hypothesis/nextAction/cadence/evidenceOfSuccess) from a
     * transformation's context. Nothing is persisted from this call alone — per ADR-008, the caller
     * must route the result through the normal explicit-review/accept experiment-creation flow.
     * See ADR-016 (Phase 5 slice C).
     */
    AiExperimentDraft proposeExperiment(String context);

    /**
     * Continue a reflection chat: given the transcript so far, produce the AI's next message (a
     * clarifying question or a closing remark). Stateless — the caller passes the full transcript
     * each time; nothing is persisted by this call. See ADR-017 (Phase 5 slice D).
     */
    AiSuggestion continueReflectionChat(String context);

    /**
     * Structure a finished reflection chat transcript into the same four fields a reflection is
     * normally persisted with. Nothing is persisted by this call alone — per ADR-008, the caller
     * must route the result through the normal explicit-review/accept flow (the existing
     * POST /api/v1/experiments/{id}/reflections endpoint) before anything is saved. See ADR-017.
     */
    AiReflectionStructure structureReflection(String context);

    /**
     * Propose a single candidate memory statement — a durable fact, pattern, or preference about
     * the user worth remembering for future context — from a reflection (and its surrounding
     * experiment context). Distinct from a wisdom "lesson": memory is about the user themselves,
     * not a takeaway from the experiment. Nothing is persisted by this call alone — per ADR-008,
     * the caller must route the result through the existing memory-proposal create endpoint
     * (landing as PROPOSED) before it becomes a real memory. See ADR-018 (Phase 6).
     */
    AiMemoryProposal proposeMemory(String context);

    record AiSuggestion(String text, String provider, String model, String promptVersion, boolean deterministicFallback) {}

    record AiWeeklySummary(String summary, String assistance, String provider, String model, boolean deterministicFallback) {}

    record AiExperimentDraft(String title, String hypothesis, String nextAction, String cadence,
                             String evidenceOfSuccess, String provider, String model, boolean deterministicFallback) {}

    record AiReflectionStructure(
        String content,
        Boolean attempted,
        String noticed,
        String evidenceNoted,
        String surprise,
        String provider,
        String model,
        boolean deterministicFallback
    ) {}

    record AiMemoryProposal(String statement, String provider, String model, boolean deterministicFallback) {}
}
