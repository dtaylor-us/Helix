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
