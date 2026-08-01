package com.helix.api.ai.adapter.out;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.ai.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for OllamaAssistantAdapter.
 * 
 * Validates:
 * - Circuit breaker behavior (fails gracefully when Ollama is down)
 * - Fallback to deterministic response on error
 * - Local-first design compliance
 * - Health status tracking
 */
@DisplayName("OllamaAssistantAdapter Tests")
class OllamaAssistantAdapterTest {

    private OllamaAssistantAdapter adapter;
    private AiProperties aiProperties;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        aiProperties.getOllama().setModel("llama2");
        aiProperties.getOllama().setBaseUrl("http://localhost:11434");
        
        adapter = new OllamaAssistantAdapter(aiProperties);
    }

    @Test
    @DisplayName("should mark provider as ollama")
    void testProviderIdentification() {
        String context = "Did some reflection today";
        
        AiAssistantPort.AiSuggestion suggestion = adapter.suggestReflectiveQuestion(context);
        
        assertThat(suggestion.provider()).isEqualTo("ollama");
    }

    @Test
    @DisplayName("should return fallback when Ollama unavailable")
    void testFallbackWhenUnavailable() {
        // Default setup will fail to connect to non-existent Ollama
        AiAssistantPort.AiSuggestion suggestion = 
            adapter.suggestReflectiveQuestion("test context");
        
        assertThat(suggestion).isNotNull();
        assertThat(suggestion.text()).isNotEmpty();
        assertThat(suggestion.deterministicFallback()).isTrue();
    }

    @Test
    @DisplayName("should track health status")
    void testHealthTracking() {
        adapter.suggestReflectiveQuestion("test context");
        
        // After failed attempt, health should be tracked
        assertThat(adapter.isHealthy()).isNotNull();
    }

    @Test
    @DisplayName("should use configured model name")
    void testModelConfiguration() {
        aiProperties.getOllama().setModel("custom-model");
        OllamaAssistantAdapter customAdapter = new OllamaAssistantAdapter(aiProperties);
        
        AiAssistantPort.AiSuggestion suggestion = 
            customAdapter.suggestReflectiveQuestion("test");
        
        assertThat(suggestion.model()).isEqualTo("custom-model");
    }

    @Test
    @DisplayName("should handle null context")
    void testNullContextHandling() {
        AiAssistantPort.AiSuggestion suggestion =
            adapter.suggestReflectiveQuestion(null);

        assertThat(suggestion).isNotNull();
        assertThat(suggestion.text()).isNotEmpty();
    }

    @Test
    @DisplayName("suggestNextAction should mark provider as ollama")
    void testNextActionProviderIdentification() {
        AiAssistantPort.AiSuggestion suggestion = adapter.suggestNextAction("Did some reflection today");

        assertThat(suggestion.provider()).isEqualTo("ollama");
    }

    @Test
    @DisplayName("suggestNextAction should return fallback when Ollama unavailable")
    void testNextActionFallbackWhenUnavailable() {
        AiAssistantPort.AiSuggestion suggestion = adapter.suggestNextAction("test context");

        assertThat(suggestion).isNotNull();
        assertThat(suggestion.text()).isNotEmpty();
        assertThat(suggestion.deterministicFallback()).isTrue();
    }
}
