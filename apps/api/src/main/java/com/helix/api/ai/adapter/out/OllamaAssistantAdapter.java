package com.helix.api.ai.adapter.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.ai.config.AiProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Ollama-based AI assistant adapter for local on-device inference.
 * 
 * Implements AiAssistantPort via Ollama API.
 * Governed by ADR-007 (local-first approach) and ADR-008 (user governance).
 * 
 * Configuration (application.yml):
 *   helix.ai.ollama.base-url: http://localhost:11434
 *   helix.ai.ollama.model: llama2
 */
@Component("ollamaAdapter")
public class OllamaAssistantAdapter implements AiAssistantPort {
    
    private final RestClient restClient;
    private final AiProperties aiProperties;
    
    // Cached availability state
    private volatile boolean isAvailable = true;
    private volatile long lastFailureTime = 0;
    private static final long AVAILABILITY_RESET_MS = 30_000; // 30 seconds

    public OllamaAssistantAdapter(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.builder()
            .baseUrl(aiProperties.getOllama().getBaseUrl())
            .build();
    }

    @Override
    public AiSuggestion suggestReflectiveQuestion(String context) {
        // If provider has been failing, return fallback to prevent cascading failures
        if (!isAvailable && System.currentTimeMillis() - lastFailureTime < AVAILABILITY_RESET_MS) {
            return createFallbackSuggestion();
        }

        try {
            OllamaRequest request = buildRequest(context);
            OllamaResponse response = restClient.post()
                .uri("/api/generate")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                    (httpRequest, httpResponse) -> {
                        throw new OllamaException("Ollama API error: " + httpResponse.getStatusCode());
                    })
                .toEntity(OllamaResponse.class)
                .getBody();

            if (response == null || response.response == null || response.response.isEmpty()) {
                recordFailure();
                return createFallbackSuggestion();
            }

            isAvailable = true; // Reset availability on success
            return new AiSuggestion(
                response.response.trim(),
                "ollama",
                aiProperties.getOllama().getModel(),
                "v1",
                false
            );
        } catch (RestClientException | OllamaException e) {
            recordFailure();
            return createFallbackSuggestion();
        }
    }

    @Override
    public AiSuggestion suggestNextAction(String context) {
        if (!isAvailable && System.currentTimeMillis() - lastFailureTime < AVAILABILITY_RESET_MS) {
            return createNextActionFallback();
        }

        try {
            OllamaRequest request = buildNextActionRequest(context);
            OllamaResponse response = restClient.post()
                .uri("/api/generate")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                    (httpRequest, httpResponse) -> {
                        throw new OllamaException("Ollama API error: " + httpResponse.getStatusCode());
                    })
                .toEntity(OllamaResponse.class)
                .getBody();

            if (response == null || response.response == null || response.response.isEmpty()) {
                recordFailure();
                return createNextActionFallback();
            }

            isAvailable = true;
            return new AiSuggestion(
                response.response.trim(),
                "ollama",
                aiProperties.getOllama().getModel(),
                "v1",
                false
            );
        } catch (RestClientException | OllamaException e) {
            recordFailure();
            return createNextActionFallback();
        }
    }

    @Override
    public AiWeeklySummary summarizeWeek(String context) {
        if (!isAvailable && System.currentTimeMillis() - lastFailureTime < AVAILABILITY_RESET_MS) {
            return createWeeklySummaryFallback();
        }

        try {
            OllamaRequest request = buildWeeklySummaryRequest(context);
            OllamaResponse response = restClient.post()
                .uri("/api/generate")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                    (httpRequest, httpResponse) -> {
                        throw new OllamaException("Ollama API error: " + httpResponse.getStatusCode());
                    })
                .toEntity(OllamaResponse.class)
                .getBody();

            if (response == null || response.response == null || response.response.isEmpty()) {
                recordFailure();
                return createWeeklySummaryFallback();
            }

            String summary = extractLabeledLine(response.response, "SUMMARY");
            String assistance = extractLabeledLine(response.response, "NEXT");
            if (summary == null || summary.isBlank() || assistance == null || assistance.isBlank()) {
                recordFailure();
                return createWeeklySummaryFallback();
            }

            isAvailable = true;
            return new AiWeeklySummary(summary, assistance, "ollama", aiProperties.getOllama().getModel(), false);
        } catch (RestClientException | OllamaException e) {
            recordFailure();
            return createWeeklySummaryFallback();
        }
    }

    @Override
    public AiExperimentDraft proposeExperiment(String context) {
        if (!isAvailable && System.currentTimeMillis() - lastFailureTime < AVAILABILITY_RESET_MS) {
            return createExperimentDraftFallback();
        }

        try {
            OllamaRequest request = buildExperimentDraftRequest(context);
            OllamaResponse response = restClient.post()
                .uri("/api/generate")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                    (httpRequest, httpResponse) -> {
                        throw new OllamaException("Ollama API error: " + httpResponse.getStatusCode());
                    })
                .toEntity(OllamaResponse.class)
                .getBody();

            if (response == null || response.response == null || response.response.isEmpty()) {
                recordFailure();
                return createExperimentDraftFallback();
            }

            String title = extractLabeledLine(response.response, "TITLE");
            if (title == null || title.isBlank()) {
                recordFailure();
                return createExperimentDraftFallback();
            }

            isAvailable = true;
            return new AiExperimentDraft(
                title,
                extractLabeledLine(response.response, "HYPOTHESIS"),
                extractLabeledLine(response.response, "NEXT_ACTION"),
                extractLabeledLine(response.response, "CADENCE"),
                extractLabeledLine(response.response, "EVIDENCE"),
                "ollama",
                aiProperties.getOllama().getModel(),
                false
            );
        } catch (RestClientException | OllamaException e) {
            recordFailure();
            return createExperimentDraftFallback();
        }
    }

    @Override
    public AiSuggestion continueReflectionChat(String context) {
        if (!isAvailable && System.currentTimeMillis() - lastFailureTime < AVAILABILITY_RESET_MS) {
            return createReflectionChatFallback();
        }

        try {
            OllamaRequest request = buildReflectionChatRequest(context);
            OllamaResponse response = restClient.post()
                .uri("/api/generate")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                    (httpRequest, httpResponse) -> {
                        throw new OllamaException("Ollama API error: " + httpResponse.getStatusCode());
                    })
                .toEntity(OllamaResponse.class)
                .getBody();

            if (response == null || response.response == null || response.response.isEmpty()) {
                recordFailure();
                return createReflectionChatFallback();
            }

            isAvailable = true;
            return new AiSuggestion(
                response.response.trim(),
                "ollama",
                aiProperties.getOllama().getModel(),
                "v1",
                false
            );
        } catch (RestClientException | OllamaException e) {
            recordFailure();
            return createReflectionChatFallback();
        }
    }

    @Override
    public AiReflectionStructure structureReflection(String context) {
        if (!isAvailable && System.currentTimeMillis() - lastFailureTime < AVAILABILITY_RESET_MS) {
            return createReflectionStructureFallback();
        }

        try {
            OllamaRequest request = buildReflectionStructureRequest(context);
            OllamaResponse response = restClient.post()
                .uri("/api/generate")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                    (httpRequest, httpResponse) -> {
                        throw new OllamaException("Ollama API error: " + httpResponse.getStatusCode());
                    })
                .toEntity(OllamaResponse.class)
                .getBody();

            if (response == null || response.response == null || response.response.isEmpty()) {
                recordFailure();
                return createReflectionStructureFallback();
            }

            String content = extractLabeledLine(response.response, "CONTENT");
            if (content == null || content.isBlank()) {
                recordFailure();
                return createReflectionStructureFallback();
            }

            isAvailable = true;
            return new AiReflectionStructure(
                content,
                parseAttempted(extractLabeledLine(response.response, "ATTEMPTED")),
                extractLabeledLine(response.response, "NOTICED"),
                extractLabeledLine(response.response, "EVIDENCE"),
                extractLabeledLine(response.response, "SURPRISE"),
                "ollama",
                aiProperties.getOllama().getModel(),
                false
            );
        } catch (RestClientException | OllamaException e) {
            recordFailure();
            return createReflectionStructureFallback();
        }
    }

    @Override
    public AiMemoryProposal proposeMemory(String context) {
        if (!isAvailable && System.currentTimeMillis() - lastFailureTime < AVAILABILITY_RESET_MS) {
            return createMemoryProposalFallback();
        }

        try {
            OllamaRequest request = buildMemoryProposalRequest(context);
            OllamaResponse response = restClient.post()
                .uri("/api/generate")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                    (httpRequest, httpResponse) -> {
                        throw new OllamaException("Ollama API error: " + httpResponse.getStatusCode());
                    })
                .toEntity(OllamaResponse.class)
                .getBody();

            if (response == null || response.response == null || response.response.isEmpty()) {
                recordFailure();
                return createMemoryProposalFallback();
            }

            String statement = response.response.trim();
            if (statement.isBlank()) {
                recordFailure();
                return createMemoryProposalFallback();
            }

            isAvailable = true;
            // A model that decides there's nothing durable worth remembering isn't a failure — it's
            // a legitimate live answer that just has no statement to propose.
            if (statement.equalsIgnoreCase("NONE")) {
                return new AiMemoryProposal(null, "ollama", aiProperties.getOllama().getModel(), false);
            }
            return new AiMemoryProposal(statement, "ollama", aiProperties.getOllama().getModel(), false);
        } catch (RestClientException | OllamaException e) {
            recordFailure();
            return createMemoryProposalFallback();
        }
    }

    @Override
    public AiRelationshipProposal proposeBeliefRelationship(String context) {
        if (!isAvailable && System.currentTimeMillis() - lastFailureTime < AVAILABILITY_RESET_MS) {
            return createRelationshipProposalFallback();
        }

        try {
            OllamaRequest request = buildRelationshipProposalRequest(context);
            OllamaResponse response = restClient.post()
                .uri("/api/generate")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                    (httpRequest, httpResponse) -> {
                        throw new OllamaException("Ollama API error: " + httpResponse.getStatusCode());
                    })
                .toEntity(OllamaResponse.class)
                .getBody();

            if (response == null || response.response == null || response.response.isEmpty()) {
                recordFailure();
                return createRelationshipProposalFallback();
            }

            String related = extractLabeledLine(response.response, "RELATED");
            if (related == null || related.isBlank()) {
                recordFailure();
                return createRelationshipProposalFallback();
            }

            isAvailable = true;
            boolean isRelated = related.trim().equalsIgnoreCase("yes");
            String explanation = isRelated ? extractLabeledLine(response.response, "WHY") : null;
            return new AiRelationshipProposal(isRelated, explanation, "ollama", aiProperties.getOllama().getModel(), false);
        } catch (RestClientException | OllamaException e) {
            recordFailure();
            return createRelationshipProposalFallback();
        }
    }

    private OllamaRequest buildRelationshipProposalRequest(String context) {
        String prompt = """
            You are helping notice thematic connections between two of someone's personal beliefs
            that aren't already linked by an explicit record in their data. Only say they're related
            if there's a genuine, specific thematic echo (e.g. the same underlying fear, the same
            kind of situation, one seeming like an evolution of the other) -- not just because both
            are beliefs, or both mention a similar generic topic. Respond with EXACTLY these labeled
            lines, no markdown, no preamble:
            RELATED: <yes or no>
            WHY: <if yes, one short sentence explaining the specific connection; omit this line if no>

            Context: %s""".formatted(context);

        OllamaRequest request = new OllamaRequest();
        request.model = aiProperties.getOllama().getModel();
        request.prompt = prompt;
        request.stream = false;
        request.temperature = aiProperties.getOllama().getTemperature();
        return request;
    }

    private AiRelationshipProposal createRelationshipProposalFallback() {
        return new AiRelationshipProposal(false, null, "ollama", aiProperties.getOllama().getModel(), true);
    }

    /**
     * Check if this adapter is currently available.
     * Implements circuit-breaker pattern to prevent cascading failures.
     */
    public boolean isHealthy() {
        if (!isAvailable && System.currentTimeMillis() - lastFailureTime >= AVAILABILITY_RESET_MS) {
            isAvailable = true; // Reset after timeout
        }
        return isAvailable;
    }

    private OllamaRequest buildRequest(String context) {
        String prompt = """
            You are a thoughtful, reflective coach helping someone explore their personal growth journey.
            Generate a single reflective question that helps the user examine their experiment or transformation.
            Keep the question open-ended, compassionate, and actionable.
            Respond with ONLY the question, no preamble or explanation.
            
            Context: %s
            
            Question:""".formatted(context);

        OllamaRequest request = new OllamaRequest();
        request.model = aiProperties.getOllama().getModel();
        request.prompt = prompt;
        request.stream = false;
        request.temperature = aiProperties.getOllama().getTemperature();
        return request;
    }

    private AiSuggestion createFallbackSuggestion() {
        return new AiSuggestion(
            "What felt lighter or heavier after today's experiment?",
            "ollama",
            aiProperties.getOllama().getModel(),
            "v1",
            true
        );
    }

    private OllamaRequest buildNextActionRequest(String context) {
        String prompt = """
            You are a behavior-change coach helping someone follow through on a small experiment
            tied to a personal transformation. Based on their experiment and latest reflection,
            propose exactly ONE small, concrete next action they could try next.
            Keep it to a single imperative sentence, at most 40 words, specific enough to act on
            today or tomorrow. Respond with ONLY the action text: no preamble, no quotation marks,
            no numbering.

            Context: %s

            Next action:""".formatted(context);

        OllamaRequest request = new OllamaRequest();
        request.model = aiProperties.getOllama().getModel();
        request.prompt = prompt;
        request.stream = false;
        request.temperature = aiProperties.getOllama().getTemperature();
        return request;
    }

    private AiSuggestion createNextActionFallback() {
        return new AiSuggestion(
            "Try repeating today's experiment on a smaller scale tomorrow.",
            "ollama",
            aiProperties.getOllama().getModel(),
            "v1",
            true
        );
    }

    private OllamaRequest buildWeeklySummaryRequest(String context) {
        String prompt = """
            You are a thoughtful personal-growth coach writing a short weekly retrospective from a
            list of the user's reflection excerpts. Respond with EXACTLY two lines, no preamble, no
            markdown, in this format:
            SUMMARY: <one short paragraph (2-3 sentences) narrating the week in a warm, honest, non-judgmental tone>
            NEXT: <one specific, concrete sentence suggesting what to try or focus on next week>

            Context: %s""".formatted(context);

        OllamaRequest request = new OllamaRequest();
        request.model = aiProperties.getOllama().getModel();
        request.prompt = prompt;
        request.stream = false;
        request.temperature = aiProperties.getOllama().getTemperature();
        return request;
    }

    private AiWeeklySummary createWeeklySummaryFallback() {
        return new AiWeeklySummary(
            "This week's reflections are recorded below.",
            "Choose one recurring pattern and run a smaller experiment next week.",
            "ollama",
            aiProperties.getOllama().getModel(),
            true
        );
    }

    private OllamaRequest buildExperimentDraftRequest(String context) {
        String prompt = """
            You are a behavior-change coach helping someone design one small experiment for a
            personal transformation they've described. Respond with EXACTLY these labeled lines, no
            preamble, no markdown, omitting a line only if you have nothing useful to add for it:
            TITLE: <short experiment title, at most 20 words>
            HYPOTHESIS: <one sentence: if I do X, then Y>
            NEXT_ACTION: <one small, concrete, immediately actionable step>
            CADENCE: <how often, e.g. "daily" or "3x this week">
            EVIDENCE: <one sentence describing what would count as useful evidence of progress>

            Context: %s""".formatted(context);

        OllamaRequest request = new OllamaRequest();
        request.model = aiProperties.getOllama().getModel();
        request.prompt = prompt;
        request.stream = false;
        request.temperature = aiProperties.getOllama().getTemperature();
        return request;
    }

    private AiExperimentDraft createExperimentDraftFallback() {
        return new AiExperimentDraft(
            "Try one small step this week",
            null,
            "Spend five minutes today on the smallest version of this.",
            null,
            null,
            "ollama",
            aiProperties.getOllama().getModel(),
            true
        );
    }

    private OllamaRequest buildReflectionChatRequest(String context) {
        String prompt = """
            You are a warm, concise reflection coach helping someone process today's experiment.
            Given the conversation so far, reply with exactly one short, natural follow-up question
            that helps clarify what happened or what they noticed. If no further clarification seems
            useful, reply with one short encouraging closing remark instead.
            Keep your response to 1-2 sentences, no markdown, no preamble.

            Conversation:
            %s""".formatted(context);

        OllamaRequest request = new OllamaRequest();
        request.model = aiProperties.getOllama().getModel();
        request.prompt = prompt;
        request.stream = false;
        request.temperature = aiProperties.getOllama().getTemperature();
        return request;
    }

    private AiSuggestion createReflectionChatFallback() {
        return new AiSuggestion(
            "What else stood out about today?",
            "ollama",
            aiProperties.getOllama().getModel(),
            "v1",
            true
        );
    }

    private OllamaRequest buildReflectionStructureRequest(String context) {
        String prompt = """
            You are helping structure a completed reflection chat for later user review before save.
            Respond with EXACTLY these labeled lines, no markdown, no preamble:
            CONTENT: <a first-person narrative summary of what happened, written as if the user said it>
            ATTEMPTED: <yes or no>
            NOTICED: <what they noticed internally, or omit if nothing useful>
            EVIDENCE: <what evidence this gave them, or omit if nothing useful>
            SURPRISE: <what surprised them, or omit if nothing useful>

            Conversation:
            %s""".formatted(context);

        OllamaRequest request = new OllamaRequest();
        request.model = aiProperties.getOllama().getModel();
        request.prompt = prompt;
        request.stream = false;
        request.temperature = aiProperties.getOllama().getTemperature();
        return request;
    }

    private AiReflectionStructure createReflectionStructureFallback() {
        return new AiReflectionStructure(
            "I reflected on what happened today and noticed a few meaningful moments.",
            null,
            null,
            null,
            null,
            "ollama",
            aiProperties.getOllama().getModel(),
            true
        );
    }

    private OllamaRequest buildMemoryProposalRequest(String context) {
        String prompt = """
            You are helping someone notice durable facts, patterns, or preferences about themselves
            worth remembering long-term — not a lesson from a single experiment (that's captured
            elsewhere as "wisdom"), but something true about who they are or how they operate that
            would be useful context in future conversations. Based on their reflection, propose
            exactly ONE such statement, written in the first person, at most 30 words. If nothing in
            the reflection reveals a genuine durable pattern (as opposed to a one-off event), respond
            with exactly: NONE. Respond with ONLY the statement or NONE — no preamble, no quotation
            marks, no labels.

            Reflection: %s""".formatted(context);

        OllamaRequest request = new OllamaRequest();
        request.model = aiProperties.getOllama().getModel();
        request.prompt = prompt;
        request.stream = false;
        request.temperature = aiProperties.getOllama().getTemperature();
        return request;
    }

    private AiMemoryProposal createMemoryProposalFallback() {
        return new AiMemoryProposal(null, "ollama", aiProperties.getOllama().getModel(), true);
    }

    /**
     * Extract the value of a "LABEL: value" line from a model response. Returns null if the label
     * isn't present. Only looks at the first matching line (responses are prompted to be single-line
     * per label); a caller treating a missing required label as a parse failure is expected.
     */
    private static String extractLabeledLine(String text, String label) {
        if (text == null) return null;
        String prefix = label + ":";
        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) {
                return trimmed.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    private static Boolean parseAttempted(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toLowerCase();
        return switch (normalized) {
            case "yes", "true" -> true;
            case "no", "false" -> false;
            default -> null;
        };
    }

    private void recordFailure() {
        isAvailable = false;
        lastFailureTime = System.currentTimeMillis();
    }

    // Ollama API Request/Response models
    static class OllamaRequest {
        @JsonProperty("model")
        String model;
        
        @JsonProperty("prompt")
        String prompt;
        
        @JsonProperty("stream")
        boolean stream;
        
        @JsonProperty("temperature")
        double temperature;
    }

    static class OllamaResponse {
        @JsonProperty("response")
        String response;
        
        @JsonProperty("done")
        boolean done;
    }

    static class OllamaException extends RuntimeException {
        OllamaException(String message) {
            super(message);
        }

        OllamaException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
