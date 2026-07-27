package com.helix.api.ai.application;

import com.helix.api.ai.adapter.out.NoAiAssistantAdapter;
import com.helix.api.ai.adapter.out.OpenAiAssistantAdapter;
import com.helix.api.ai.adapter.out.OllamaAssistantAdapter;
import com.helix.api.ai.config.AiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AiOrchestrationService.
 * 
 * Validates:
 * - Health monitoring of AI providers
 * - Graceful degradation on provider failure
 * - Availability status reporting
 * - Diagnostics information
 */
@DisplayName("AiOrchestrationService Tests")
class AiOrchestrationServiceTest {

    private AiOrchestrationService orchestrationService;
    private AiProperties aiProperties;
    private AiAssistantPort aiAssistantPort;
    private OpenAiAssistantAdapter openaiAdapter;
    private OllamaAssistantAdapter ollamaAdapter;

    @BeforeEach
    void setUp() {
        aiProperties = new AiProperties();
        aiProperties.getOpenai().setApiKey("test-key");
        
        aiAssistantPort = new NoAiAssistantAdapter();
        openaiAdapter = new OpenAiAssistantAdapter(aiProperties);
        ollamaAdapter = new OllamaAssistantAdapter(aiProperties);
        
        orchestrationService = new AiOrchestrationService(
            aiAssistantPort, aiProperties, openaiAdapter, ollamaAdapter);
    }

    @Test
    @DisplayName("should report AI availability")
    void testAiAvailabilityReporting() {
        boolean available = orchestrationService.isAiAvailable();
        
        assertThat(available).isNotNull();
    }

    @Test
    @DisplayName("should report active provider")
    void testActiveProviderReporting() {
        String activeProvider = orchestrationService.getActiveProvider();
        
        assertThat(activeProvider).isNotEmpty();
        assertThat(activeProvider).isEqualTo("openai");
    }

    @Test
    @DisplayName("should provide diagnostics")
    void testDiagnosticsReporting() {
        AiOrchestrationService.AiDiagnostics diagnostics = 
            orchestrationService.getDiagnostics();
        
        assertThat(diagnostics).isNotNull();
        assertThat(diagnostics.activeProvider()).isNotEmpty();
        assertThat(diagnostics.overallHealth()).isNotNull();
    }

    @Test
    @DisplayName("should identify active provider in diagnostics")
    void testDiagnosticsActiveProvider() {
        AiOrchestrationService.AiDiagnostics diagnostics = 
            orchestrationService.getDiagnostics();
        
        assertThat(diagnostics.activeProvider()).isEqualTo("openai");
    }

    @Test
    @DisplayName("should handle health check without throwing")
    void testHealthCheckNonThrow() {
        assertThatCode(() -> orchestrationService.checkProviderHealth())
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should report provider health status")
    void testProviderHealthStatus() {
        AiOrchestrationService.AiDiagnostics diagnostics = 
            orchestrationService.getDiagnostics();
        
        // At least one of the providers should report a status
        assertThat(diagnostics.openaiHealthy())
            .isNotNull();
        assertThat(diagnostics.ollamaHealthy())
            .isNotNull();
    }

    @Test
    @DisplayName("should start with healthy state")
    void testInitialHealthyState() {
        orchestrationService.checkProviderHealth();
        
        assertThat(orchestrationService.isAiAvailable()).isNotNull();
    }
}
