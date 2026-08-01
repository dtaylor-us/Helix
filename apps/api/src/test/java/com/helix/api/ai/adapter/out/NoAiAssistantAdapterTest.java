package com.helix.api.ai.adapter.out;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.ai.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for NoAiAssistantAdapter.
 * 
 * Validates:
 * - Deterministic fallback behavior
 * - Consistent response structure
 * - No external dependencies
 * - Compliance with AiAssistantPort contract
 */
@DisplayName("NoAiAssistantAdapter Tests")
class NoAiAssistantAdapterTest {

    private NoAiAssistantAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new NoAiAssistantAdapter();
    }

    @Test
    @DisplayName("should return deterministic fallback suggestion")
    void testDeterministicFallback() {
        String context = "I tried meditation for 5 minutes today";
        
        AiAssistantPort.AiSuggestion suggestion = adapter.suggestReflectiveQuestion(context);
        
        assertThat(suggestion).isNotNull();
        assertThat(suggestion.text()).isNotEmpty();
        assertThat(suggestion.provider()).isEqualTo("none");
        assertThat(suggestion.deterministicFallback()).isTrue();
    }

    @Test
    @DisplayName("should return same suggestion regardless of context")
    void testConsistentResponse() {
        AiAssistantPort.AiSuggestion suggestion1 = 
            adapter.suggestReflectiveQuestion("context 1");
        AiAssistantPort.AiSuggestion suggestion2 = 
            adapter.suggestReflectiveQuestion("context 2");
        
        assertThat(suggestion1.text()).isEqualTo(suggestion2.text());
    }

    @Test
    @DisplayName("should handle null context gracefully")
    void testNullContextHandling() {
        AiAssistantPort.AiSuggestion suggestion = adapter.suggestReflectiveQuestion(null);
        
        assertThat(suggestion).isNotNull();
        assertThat(suggestion.text()).isNotEmpty();
    }

    @Test
    @DisplayName("should mark response as fallback")
    void testFallbackFlag() {
        AiAssistantPort.AiSuggestion suggestion =
            adapter.suggestReflectiveQuestion("any context");

        assertThat(suggestion.deterministicFallback()).isTrue();
    }

    @Test
    @DisplayName("suggestNextAction should return a deterministic fallback distinct from the reflective question")
    void testSuggestNextActionFallback() {
        AiAssistantPort.AiSuggestion question = adapter.suggestReflectiveQuestion("context");
        AiAssistantPort.AiSuggestion action = adapter.suggestNextAction("context");

        assertThat(action).isNotNull();
        assertThat(action.text()).isNotEmpty();
        assertThat(action.provider()).isEqualTo("none");
        assertThat(action.deterministicFallback()).isTrue();
        assertThat(action.text()).isNotEqualTo(question.text());
    }

    @Test
    @DisplayName("reflection chat methods should return deterministic fallback responses")
    void testReflectionChatFallbacks() {
        AiAssistantPort.AiSuggestion turn = adapter.continueReflectionChat("User: I paused once.");
        AiAssistantPort.AiReflectionStructure structure = adapter.structureReflection("User: I paused once.");

        assertThat(turn.provider()).isEqualTo("none");
        assertThat(turn.deterministicFallback()).isTrue();
        assertThat(turn.text()).isNotEmpty();

        assertThat(structure.provider()).isEqualTo("none");
        assertThat(structure.model()).isEqualTo("deterministic");
        assertThat(structure.deterministicFallback()).isTrue();
        assertThat(structure.content()).isNotBlank();
    }
}
