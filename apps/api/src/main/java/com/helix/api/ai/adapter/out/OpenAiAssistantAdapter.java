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
