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
        String transformationTitle = extractTransformationTitle(context);
        return new AiExperimentDraft(
            trimToLength("First small step toward " + transformationTitle, 180),
            "A smaller, repeatable action will help me learn what actually moves this transformation forward.",
            "Choose one action you can finish in under ten minutes and try it once today.",
            "Once today",
            "You notice one concrete sign that this transformation felt easier to practice.",
            "none",
            "deterministic",
            true
        );
    }

    private String extractTransformationTitle(String context) {
        if (context != null) {
            for (String line : context.lines().toList()) {
                if (line.startsWith("Transformation: ") && !line.substring(16).isBlank()) {
                    return line.substring(16).trim();
                }
            }
        }
        return "this transformation";
    }

    private String trimToLength(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
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

    // Unlike the other generative surfaces, memory proposals have no generic deterministic
    // statement worth showing — a made-up "fact about the user" template would be actively
    // misleading, not just less personalized. With no AI provider configured (or during an
    // outage), there's simply nothing to propose; the caller treats a null statement as "no
    // card to show" rather than falling back to placeholder content.
    @Override
    public AiMemoryProposal proposeMemory(String context) {
        return new AiMemoryProposal(null, "none", "deterministic", true);
    }

    // Same reasoning as proposeMemory above: with no AI provider configured, there's no
    // trustworthy way to judge a thematic connection between two beliefs. Defaulting to "not
    // related" rather than fabricating one keeps this feature dormant, not misleading, when no
    // provider is configured.
    @Override
    public AiRelationshipProposal proposeBeliefRelationship(String context) {
        return new AiRelationshipProposal(false, null, "none", "deterministic", true);
    }
}
