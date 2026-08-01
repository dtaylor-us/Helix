package com.helix.api.ai.adapter.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;
    
    // Cached availability state
    private volatile boolean isAvailable = true;
    private volatile long lastFailureTime = 0;
    private static final long AVAILABILITY_RESET_MS = 30_000; // 30 seconds

    public OllamaAssistantAdapter(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.objectMapper = new ObjectMapper();
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
    public AiExperimentDraft proposeExperiment(ExperimentDraftRequest request) {
        if (!isAvailable && System.currentTimeMillis() - lastFailureTime < AVAILABILITY_RESET_MS) {
            return createExperimentFallback(request);
        }

        try {
            OllamaRequest ollamaRequest = buildExperimentDraftRequest(request);
            OllamaResponse response = restClient.post()
                .uri("/api/generate")
                .body(ollamaRequest)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                    (httpRequest, httpResponse) -> {
                        throw new OllamaException("Ollama API error: " + httpResponse.getStatusCode());
                    })
                .toEntity(OllamaResponse.class)
                .getBody();

            if (response == null || response.response == null || response.response.isEmpty()) {
                recordFailure();
                return createExperimentFallback(request);
            }

            AiExperimentDraft parsedDraft = parseExperimentDraft(response.response);
            if (parsedDraft == null || !hasText(parsedDraft.title()) || !hasText(parsedDraft.hypothesis()) || !hasText(parsedDraft.nextAction())) {
                recordFailure();
                return createExperimentFallback(request);
            }

            isAvailable = true;
            return new AiExperimentDraft(
                parsedDraft.title().trim(),
                parsedDraft.hypothesis().trim(),
                parsedDraft.nextAction().trim(),
                normalizeOptional(parsedDraft.cadence()),
                normalizeOptional(parsedDraft.evidenceOfSuccess()),
                "ollama",
                aiProperties.getOllama().getModel(),
                "v1",
                false
            );
        } catch (RestClientException | OllamaException e) {
            recordFailure();
            return createExperimentFallback(request);
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

    private OllamaRequest buildExperimentDraftRequest(ExperimentDraftRequest request) {
        String prompt = """
            You are a behavior-change coach helping someone define a small experiment for a personal transformation.
            Return ONLY valid JSON with these keys: title, hypothesis, nextAction, cadence, evidenceOfSuccess.
            Requirements:
            - title: concise, under 180 characters
            - hypothesis: 1-2 sentences about what the user wants to learn
            - nextAction: one concrete first step
            - cadence: a short phrase, or an empty string if unclear
            - evidenceOfSuccess: one short observable sign, or an empty string if unclear
            Do not include markdown, code fences, or extra commentary.

            Transformation title: %s
            Purpose: %s
            Desired identity: %s
            Obstacle: %s
            """.formatted(
            defaultValue(request.transformationTitle()),
            defaultValue(request.purpose()),
            defaultValue(request.desiredIdentity()),
            defaultValue(request.obstacle())
        );

        OllamaRequest ollamaRequest = new OllamaRequest();
        ollamaRequest.model = aiProperties.getOllama().getModel();
        ollamaRequest.prompt = prompt;
        ollamaRequest.stream = false;
        ollamaRequest.temperature = aiProperties.getOllama().getTemperature();
        return ollamaRequest;
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

    private AiExperimentDraft parseExperimentDraft(String content) {
        try {
            ExperimentDraftPayload payload = objectMapper.readValue(stripCodeFences(content), ExperimentDraftPayload.class);
            return new AiExperimentDraft(
                payload.title,
                payload.hypothesis,
                payload.nextAction,
                payload.cadence,
                payload.evidenceOfSuccess,
                "ollama",
                aiProperties.getOllama().getModel(),
                "v1",
                false
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private AiExperimentDraft createExperimentFallback(ExperimentDraftRequest request) {
        String transformationTitle = hasText(request.transformationTitle()) ? request.transformationTitle().trim() : "this transformation";
        return new AiExperimentDraft(
            trimToLength("First small step toward " + transformationTitle, 180),
            "A smaller, repeatable action will help me learn what actually moves this transformation forward.",
            "Choose one action you can finish in under ten minutes and try it once today.",
            "Once today",
            "You notice one concrete sign that this transformation felt easier to practice.",
            "ollama",
            aiProperties.getOllama().getModel(),
            "v1",
            true
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeOptional(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String defaultValue(String value) {
        return hasText(value) ? value.trim() : "Not provided";
    }

    private String stripCodeFences(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("```json", "").replace("```", "").trim();
    }

    private String trimToLength(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void recordFailure() {
        isAvailable = false;
        lastFailureTime = System.currentTimeMillis();
    }

    static class ExperimentDraftPayload {
        @JsonProperty("title")
        String title;

        @JsonProperty("hypothesis")
        String hypothesis;

        @JsonProperty("nextAction")
        String nextAction;

        @JsonProperty("cadence")
        String cadence;

        @JsonProperty("evidenceOfSuccess")
        String evidenceOfSuccess;
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
