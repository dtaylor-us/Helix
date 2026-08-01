package com.helix.api.ai.adapter.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.ai.config.AiProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

/**
 * OpenAI-based AI assistant adapter.
 * 
 * Implements AiAssistantPort via OpenAI Chat Completions API.
 * Governed by ADR-007 (supports cloud and local deployment) and ADR-008 (user governance).
 * 
 * Configuration (application.yml):
 *   helix.ai.openai.api-key: ${OPENAI_API_KEY}
 *   helix.ai.openai.model: gpt-4o-mini
 *   helix.ai.openai.base-url: https://api.openai.com
 */
@Component("openaiAdapter")
public class OpenAiAssistantAdapter implements AiAssistantPort {
    
    private final RestClient restClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    
    // Cached availability state
    private volatile boolean isAvailable = true;
    private volatile long lastFailureTime = 0;
    private static final long AVAILABILITY_RESET_MS = 30_000; // 30 seconds

    public OpenAiAssistantAdapter(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
            .baseUrl(aiProperties.getOpenai().getBaseUrl())
            .defaultHeader("Authorization", "Bearer " + aiProperties.getOpenai().getApiKey())
            .build();
    }

    @Override
    public AiSuggestion suggestReflectiveQuestion(String context) {
        // If provider has been failing, return fallback to prevent cascading failures
        if (!isAvailable && System.currentTimeMillis() - lastFailureTime < AVAILABILITY_RESET_MS) {
            return createFallbackSuggestion(context);
        }

        try {
            OpenAiRequest request = buildRequest(context);
            OpenAiResponse response = restClient.post()
                .uri("/v1/chat/completions")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), 
                    (httpRequest, httpResponse) -> {
                        throw new OpenAiException("OpenAI API error: " + httpResponse.getStatusCode());
                    })
                .toEntity(OpenAiResponse.class)
                .getBody();

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                recordFailure();
                return createFallbackSuggestion(context);
            }

            String text = response.choices.get(0).message.content;
            isAvailable = true; // Reset availability on success
            
            return new AiSuggestion(
                text,
                "openai",
                aiProperties.getOpenai().getModel(),
                "v1",
                false
            );
        } catch (RestClientException | OpenAiException e) {
            recordFailure();
            return createFallbackSuggestion(context);
        }
    }

    @Override
    public AiSuggestion suggestNextAction(String context) {
        if (!isAvailable && System.currentTimeMillis() - lastFailureTime < AVAILABILITY_RESET_MS) {
            return createNextActionFallback();
        }

        try {
            OpenAiRequest request = buildNextActionRequest(context);
            OpenAiResponse response = restClient.post()
                .uri("/v1/chat/completions")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                    (httpRequest, httpResponse) -> {
                        throw new OpenAiException("OpenAI API error: " + httpResponse.getStatusCode());
                    })
                .toEntity(OpenAiResponse.class)
                .getBody();

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                recordFailure();
                return createNextActionFallback();
            }

            String text = response.choices.get(0).message.content;
            isAvailable = true;

            return new AiSuggestion(
                text == null ? null : text.trim(),
                "openai",
                aiProperties.getOpenai().getModel(),
                "v1",
                false
            );
        } catch (RestClientException | OpenAiException e) {
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
            OpenAiRequest request = buildWeeklySummaryRequest(context);
            OpenAiResponse response = restClient.post()
                .uri("/v1/chat/completions")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                    (httpRequest, httpResponse) -> {
                        throw new OpenAiException("OpenAI API error: " + httpResponse.getStatusCode());
                    })
                .toEntity(OpenAiResponse.class)
                .getBody();

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                recordFailure();
                return createWeeklySummaryFallback();
            }

            String text = response.choices.get(0).message.content;
            String summary = extractLabeledLine(text, "SUMMARY");
            String assistance = extractLabeledLine(text, "NEXT");
            if (summary == null || summary.isBlank() || assistance == null || assistance.isBlank()) {
                recordFailure();
                return createWeeklySummaryFallback();
            }

            isAvailable = true;
            return new AiWeeklySummary(summary, assistance, "openai", aiProperties.getOpenai().getModel(), false);
        } catch (RestClientException | OpenAiException e) {
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
            OpenAiRequest request = buildExperimentDraftRequest(context);
            OpenAiResponse response = restClient.post()
                .uri("/v1/chat/completions")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                    (httpRequest, httpResponse) -> {
                        throw new OpenAiException("OpenAI API error: " + httpResponse.getStatusCode());
                    })
                .toEntity(OpenAiResponse.class)
                .getBody();

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                recordFailure();
                return createExperimentDraftFallback();
            }

            String text = response.choices.get(0).message.content;
            String title = extractLabeledLine(text, "TITLE");
            if (title == null || title.isBlank()) {
                recordFailure();
                return createExperimentDraftFallback();
            }

            isAvailable = true;
            return new AiExperimentDraft(
                title,
                extractLabeledLine(text, "HYPOTHESIS"),
                extractLabeledLine(text, "NEXT_ACTION"),
                extractLabeledLine(text, "CADENCE"),
                extractLabeledLine(text, "EVIDENCE"),
                "openai",
                aiProperties.getOpenai().getModel(),
                false
            );
        } catch (RestClientException | OpenAiException e) {
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
            OpenAiRequest request = buildReflectionChatRequest(context);
            OpenAiResponse response = restClient.post()
                .uri("/v1/chat/completions")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                    (httpRequest, httpResponse) -> {
                        throw new OpenAiException("OpenAI API error: " + httpResponse.getStatusCode());
                    })
                .toEntity(OpenAiResponse.class)
                .getBody();

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                recordFailure();
                return createReflectionChatFallback();
            }

            String text = response.choices.get(0).message.content;
            isAvailable = true;
            return new AiSuggestion(
                text == null ? null : text.trim(),
                "openai",
                aiProperties.getOpenai().getModel(),
                "v1",
                false
            );
        } catch (RestClientException | OpenAiException e) {
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
            OpenAiRequest request = buildReflectionStructureRequest(context);
            OpenAiResponse response = restClient.post()
                .uri("/v1/chat/completions")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                    (httpRequest, httpResponse) -> {
                        throw new OpenAiException("OpenAI API error: " + httpResponse.getStatusCode());
                    })
                .toEntity(OpenAiResponse.class)
                .getBody();

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                recordFailure();
                return createReflectionStructureFallback();
            }

            String text = response.choices.get(0).message.content;
            String content = extractLabeledLine(text, "CONTENT");
            if (content == null || content.isBlank()) {
                recordFailure();
                return createReflectionStructureFallback();
            }

            isAvailable = true;
            return new AiReflectionStructure(
                content,
                parseAttempted(extractLabeledLine(text, "ATTEMPTED")),
                extractLabeledLine(text, "NOTICED"),
                extractLabeledLine(text, "EVIDENCE"),
                extractLabeledLine(text, "SURPRISE"),
                "openai",
                aiProperties.getOpenai().getModel(),
                false
            );
        } catch (RestClientException | OpenAiException e) {
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
            OpenAiRequest request = buildMemoryProposalRequest(context);
            OpenAiResponse response = restClient.post()
                .uri("/v1/chat/completions")
                .body(request)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                    (httpRequest, httpResponse) -> {
                        throw new OpenAiException("OpenAI API error: " + httpResponse.getStatusCode());
                    })
                .toEntity(OpenAiResponse.class)
                .getBody();

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                recordFailure();
                return createMemoryProposalFallback();
            }

            String text = response.choices.get(0).message.content;
            String statement = text == null ? null : text.trim();
            if (statement == null || statement.isBlank()) {
                recordFailure();
                return createMemoryProposalFallback();
            }

            isAvailable = true;
            // A model that decides there's nothing durable worth remembering isn't a failure — it's
            // a legitimate live answer that just has no statement to propose. The caller treats a
            // null statement as "nothing to show," independent of deterministicFallback.
            if (statement.equalsIgnoreCase("NONE")) {
                return new AiMemoryProposal(null, "openai", aiProperties.getOpenai().getModel(), false);
            }
            return new AiMemoryProposal(statement, "openai", aiProperties.getOpenai().getModel(), false);
        } catch (RestClientException | OpenAiException e) {
            recordFailure();
            return createMemoryProposalFallback();
        }
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

    private OpenAiRequest buildRequest(String context) {
        String systemPrompt = """
            You are a thoughtful, reflective coach helping someone explore their personal growth journey.
            Generate a single reflective question that helps the user examine their experiment or transformation.
            Keep the question open-ended, compassionate, and actionable.
            Respond with ONLY the question, no preamble or explanation.
            """;

        OpenAiRequest request = new OpenAiRequest();
        request.model = aiProperties.getOpenai().getModel();
        request.temperature = aiProperties.getOpenai().getTemperature();
        request.maxTokens = aiProperties.getOpenai().getMaxTokens();
        request.messages = List.of(
            new OpenAiRequest.Message("system", systemPrompt),
            new OpenAiRequest.Message("user", context)
        );
        return request;
    }

    private AiSuggestion createFallbackSuggestion(String context) {
        return new AiSuggestion(
            "What felt lighter or heavier after today's experiment?",
            "openai",
            aiProperties.getOpenai().getModel(),
            "v1",
            true
        );
    }

    private OpenAiRequest buildNextActionRequest(String context) {
        String systemPrompt = """
            You are a behavior-change coach helping someone follow through on a small experiment
            tied to a personal transformation. Based on their experiment and latest reflection,
            propose exactly ONE small, concrete next action they could try next.
            Keep it to a single imperative sentence, at most 40 words, specific enough to act on
            today or tomorrow. Respond with ONLY the action text: no preamble, no quotation marks,
            no numbering.
            """;

        OpenAiRequest request = new OpenAiRequest();
        request.model = aiProperties.getOpenai().getModel();
        request.temperature = aiProperties.getOpenai().getTemperature();
        request.maxTokens = aiProperties.getOpenai().getMaxTokens();
        request.messages = List.of(
            new OpenAiRequest.Message("system", systemPrompt),
            new OpenAiRequest.Message("user", context)
        );
        return request;
    }

    private AiSuggestion createNextActionFallback() {
        return new AiSuggestion(
            "Try repeating today's experiment on a smaller scale tomorrow.",
            "openai",
            aiProperties.getOpenai().getModel(),
            "v1",
            true
        );
    }

    private OpenAiRequest buildWeeklySummaryRequest(String context) {
        String systemPrompt = """
            You are a thoughtful personal-growth coach writing a short weekly retrospective from a
            list of the user's reflection excerpts. Respond with EXACTLY two lines, no preamble, no
            markdown, in this format:
            SUMMARY: <one short paragraph (2-3 sentences) narrating the week in a warm, honest, non-judgmental tone>
            NEXT: <one specific, concrete sentence suggesting what to try or focus on next week>
            """;

        OpenAiRequest request = new OpenAiRequest();
        request.model = aiProperties.getOpenai().getModel();
        request.temperature = aiProperties.getOpenai().getTemperature();
        request.maxTokens = aiProperties.getOpenai().getMaxTokens();
        request.messages = List.of(
            new OpenAiRequest.Message("system", systemPrompt),
            new OpenAiRequest.Message("user", context)
        );
        return request;
    }

    private AiWeeklySummary createWeeklySummaryFallback() {
        return new AiWeeklySummary(
            "This week's reflections are recorded below.",
            "Choose one recurring pattern and run a smaller experiment next week.",
            "openai",
            aiProperties.getOpenai().getModel(),
            true
        );
    }

    private OpenAiRequest buildExperimentDraftRequest(String context) {
        String systemPrompt = """
            You are a behavior-change coach helping someone design one small experiment for a
            personal transformation they've described. Respond with EXACTLY these labeled lines, no
            preamble, no markdown, omitting a line only if you have nothing useful to add for it:
            TITLE: <short experiment title, at most 20 words>
            HYPOTHESIS: <one sentence: if I do X, then Y>
            NEXT_ACTION: <one small, concrete, immediately actionable step>
            CADENCE: <how often, e.g. "daily" or "3x this week">
            EVIDENCE: <one sentence describing what would count as useful evidence of progress>
            """;

        OpenAiRequest request = new OpenAiRequest();
        request.model = aiProperties.getOpenai().getModel();
        request.temperature = aiProperties.getOpenai().getTemperature();
        request.maxTokens = aiProperties.getOpenai().getMaxTokens();
        request.messages = List.of(
            new OpenAiRequest.Message("system", systemPrompt),
            new OpenAiRequest.Message("user", context)
        );
        return request;
    }

    private AiExperimentDraft createExperimentDraftFallback() {
        return new AiExperimentDraft(
            "Try one small step this week",
            null,
            "Spend five minutes today on the smallest version of this.",
            null,
            null,
            "openai",
            aiProperties.getOpenai().getModel(),
            true
        );
    }

    private OpenAiRequest buildReflectionChatRequest(String context) {
        String systemPrompt = """
            You are a warm, concise reflection coach helping someone process today's experiment.
            Given the conversation so far, reply with exactly one short, natural follow-up question
            that helps clarify what happened or what they noticed. If no further clarification seems
            useful, reply with one short encouraging closing remark instead.
            Keep your response to 1-2 sentences, no markdown, no preamble.
            """;

        OpenAiRequest request = new OpenAiRequest();
        request.model = aiProperties.getOpenai().getModel();
        request.temperature = aiProperties.getOpenai().getTemperature();
        request.maxTokens = aiProperties.getOpenai().getMaxTokens();
        request.messages = List.of(
            new OpenAiRequest.Message("system", systemPrompt),
            new OpenAiRequest.Message("user", context)
        );
        return request;
    }

    private AiSuggestion createReflectionChatFallback() {
        return new AiSuggestion(
            "What else stood out about today?",
            "openai",
            aiProperties.getOpenai().getModel(),
            "v1",
            true
        );
    }

    private OpenAiRequest buildReflectionStructureRequest(String context) {
        String systemPrompt = """
            You are helping structure a completed reflection chat for later user review before save.
            Respond with EXACTLY these labeled lines, no markdown, no preamble:
            CONTENT: <a first-person narrative summary of what happened, written as if the user said it>
            ATTEMPTED: <yes or no>
            NOTICED: <what they noticed internally, or omit if nothing useful>
            EVIDENCE: <what evidence this gave them, or omit if nothing useful>
            SURPRISE: <what surprised them, or omit if nothing useful>
            """;

        OpenAiRequest request = new OpenAiRequest();
        request.model = aiProperties.getOpenai().getModel();
        request.temperature = aiProperties.getOpenai().getTemperature();
        request.maxTokens = aiProperties.getOpenai().getMaxTokens();
        request.messages = List.of(
            new OpenAiRequest.Message("system", systemPrompt),
            new OpenAiRequest.Message("user", context)
        );
        return request;
    }

    private AiReflectionStructure createReflectionStructureFallback() {
        return new AiReflectionStructure(
            "I reflected on what happened today and noticed a few meaningful moments.",
            null,
            null,
            null,
            null,
            "openai",
            aiProperties.getOpenai().getModel(),
            true
        );
    }

    private OpenAiRequest buildMemoryProposalRequest(String context) {
        String systemPrompt = """
            You are helping someone notice durable facts, patterns, or preferences about themselves
            worth remembering long-term — not a lesson from a single experiment (that's captured
            elsewhere as "wisdom"), but something true about who they are or how they operate that
            would be useful context in future conversations. Based on their reflection, propose
            exactly ONE such statement, written in the first person, at most 30 words. If nothing in
            the reflection reveals a genuine durable pattern (as opposed to a one-off event), respond
            with exactly: NONE. Respond with ONLY the statement or NONE — no preamble, no quotation
            marks, no labels.
            """;

        OpenAiRequest request = new OpenAiRequest();
        request.model = aiProperties.getOpenai().getModel();
        request.temperature = aiProperties.getOpenai().getTemperature();
        request.maxTokens = aiProperties.getOpenai().getMaxTokens();
        request.messages = List.of(
            new OpenAiRequest.Message("system", systemPrompt),
            new OpenAiRequest.Message("user", context)
        );
        return request;
    }

    private AiMemoryProposal createMemoryProposalFallback() {
        return new AiMemoryProposal(null, "openai", aiProperties.getOpenai().getModel(), true);
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

    // OpenAI API Request/Response models
    static class OpenAiRequest {
        @JsonProperty("model")
        String model;
        
        @JsonProperty("messages")
        List<Message> messages;
        
        @JsonProperty("temperature")
        double temperature;
        
        @JsonProperty("max_tokens")
        int maxTokens;

        static class Message {
            @JsonProperty("role")
            String role;
            
            @JsonProperty("content")
            String content;

            Message(String role, String content) {
                this.role = role;
                this.content = content;
            }
        }
    }

    static class OpenAiResponse {
        @JsonProperty("choices")
        List<Choice> choices;

        static class Choice {
            @JsonProperty("message")
            Message message;

            static class Message {
                @JsonProperty("content")
                String content;
            }
        }
    }

    static class OpenAiException extends RuntimeException {
        OpenAiException(String message) {
            super(message);
        }

        OpenAiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
