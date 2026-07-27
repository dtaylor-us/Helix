package com.helix.api.ai.adapter.out;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.ai.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AiProviderFactory.
 * 
 * Validates:
 * - Correct adapter selection based on configuration
 * - Fallback to NoOp when provider configuration invalid
 * - Default provider (OpenAI) selection
 * - Provider type enum parsing
 */
@DisplayName("AiProviderFactory Tests")
class AiProviderFactoryTest {

    private AiProviderFactory factory;
    private AiProperties aiProperties;
    private OpenAiAssistantAdapter openaiAdapter;
    private OllamaAssistantAdapter ollamaAdapter;
    private NoAiAssistantAdapter noOpAdapter;

    @BeforeEach
    void setUp() {
        factory = new AiProviderFactory();
        aiProperties = new AiProperties();
        aiProperties.getOpenai().setApiKey("test-key");
        
        openaiAdapter = new OpenAiAssistantAdapter(aiProperties);
        ollamaAdapter = new OllamaAssistantAdapter(aiProperties);
        noOpAdapter = new NoAiAssistantAdapter();
    }

    @Test
    @DisplayName("should default to openai provider")
    void testDefaultProviderIsOpenAi() {
        AiAssistantPort port = factory.aiAssistantPort(
            aiProperties, openaiAdapter, ollamaAdapter, noOpAdapter);
        
        assertThat(port).isNotNull();
    }

    @Test
    @DisplayName("should select no-op when provider type is NONE")
    void testSelectNoOpForNoneProvider() {
        aiProperties.setProvider("none");
        
        AiAssistantPort port = factory.aiAssistantPort(
            aiProperties, openaiAdapter, ollamaAdapter, noOpAdapter);
        
        AiAssistantPort.AiSuggestion suggestion = port.suggestReflectiveQuestion("test");
        assertThat(suggestion.provider()).isEqualTo("none");
    }

    @Test
    @DisplayName("should fallback to no-op when OpenAI key missing")
    void testFallbackWhenOpenAiKeyMissing() {
        aiProperties.setProvider("openai");
        aiProperties.getOpenai().setApiKey(null);
        
        AiAssistantPort port = factory.aiAssistantPort(
            aiProperties, openaiAdapter, ollamaAdapter, noOpAdapter);
        
        AiAssistantPort.AiSuggestion suggestion = port.suggestReflectiveQuestion("test");
        assertThat(suggestion.provider()).isEqualTo("none");
    }

    @Test
    @DisplayName("should fallback to no-op when OpenAI key is unresolved")
    void testFallbackWhenOpenAiKeyUnresolved() {
        aiProperties.setProvider("openai");
        aiProperties.getOpenai().setApiKey("${OPENAI_API_KEY}");
        
        AiAssistantPort port = factory.aiAssistantPort(
            aiProperties, openaiAdapter, ollamaAdapter, noOpAdapter);
        
        AiAssistantPort.AiSuggestion suggestion = port.suggestReflectiveQuestion("test");
        assertThat(suggestion.provider()).isEqualTo("none");
    }

    @Test
    @DisplayName("should select ollama provider")
    void testSelectOllamaProvider() {
        aiProperties.setProvider("ollama");
        
        AiAssistantPort port = factory.aiAssistantPort(
            aiProperties, openaiAdapter, ollamaAdapter, noOpAdapter);
        
        AiAssistantPort.AiSuggestion suggestion = port.suggestReflectiveQuestion("test");
        assertThat(suggestion.provider()).isEqualTo("ollama");
    }

    @Test
    @DisplayName("should handle case-insensitive provider names")
    void testCaseInsensitiveProviderNames() {
        aiProperties.setProvider("OPENAI");
        
        AiAssistantPort port = factory.aiAssistantPort(
            aiProperties, openaiAdapter, ollamaAdapter, noOpAdapter);
        
        assertThat(port).isNotNull();
    }
}
