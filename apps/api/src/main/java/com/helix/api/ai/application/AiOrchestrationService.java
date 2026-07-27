package com.helix.api.ai.application;

import com.helix.api.ai.adapter.out.OpenAiAssistantAdapter;
import com.helix.api.ai.adapter.out.OllamaAssistantAdapter;
import com.helix.api.ai.config.AiProperties;
import com.helix.api.ai.config.AiProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * AI Orchestration Service - manages provider lifecycle and availability.
 * 
 * Responsibilities:
 * 1. Health monitoring of active AI provider
 * 2. Graceful degradation when provider becomes unavailable
 * 3. Logging and observability for AI provider failures
 * 4. Recovery and retry logic
 * 
 * Governed by ADR-006 (optional AI) and ADR-008 (user governance).
 * Failures are logged but never block core application workflows.
 */
@Service
public class AiOrchestrationService {
    
    private static final Logger log = LoggerFactory.getLogger(AiOrchestrationService.class);
    
    private final AiAssistantPort aiAssistantPort;
    private final AiProperties aiProperties;
    private final OpenAiAssistantAdapter openaiAdapter;
    private final OllamaAssistantAdapter ollamaAdapter;
    
    private volatile boolean isHealthy = true;

    public AiOrchestrationService(
            AiAssistantPort aiAssistantPort,
            AiProperties aiProperties,
            OpenAiAssistantAdapter openaiAdapter,
            OllamaAssistantAdapter ollamaAdapter) {
        this.aiAssistantPort = aiAssistantPort;
        this.aiProperties = aiProperties;
        this.openaiAdapter = openaiAdapter;
        this.ollamaAdapter = ollamaAdapter;
    }

    /**
     * Get current AI availability status.
     * Used by health checks and UI to show AI availability to users.
     */
    public boolean isAiAvailable() {
        return isHealthy && getActiveProviderHealth();
    }

    /**
     * Periodic health check (every 30 seconds) to detect provider failures early.
     * Does not disrupt core application—failures are logged only.
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 5_000)
    public void checkProviderHealth() {
        try {
            AiProviderType activeProvider = AiProviderType.fromConfigValue(aiProperties.getProvider());
            
            boolean currentHealth = switch (activeProvider) {
                case OPENAI -> openaiAdapter.isHealthy();
                case OLLAMA -> ollamaAdapter.isHealthy();
                case NONE -> true; // No-op always "healthy"
            };
            
            boolean previousHealth = isHealthy;
            isHealthy = currentHealth;
            
            if (previousHealth && !currentHealth) {
                log.warn("AI Provider {} became unavailable. Core workflows will use deterministic fallback.",
                    activeProvider.configValue);
            } else if (!previousHealth && currentHealth) {
                log.info("AI Provider {} recovered.", activeProvider.configValue);
            }
        } catch (Exception e) {
            log.error("Error checking provider health", e);
            isHealthy = false;
        }
    }

    /**
     * Get health status of currently active provider.
     */
    private boolean getActiveProviderHealth() {
        try {
            AiProviderType activeProvider = AiProviderType.fromConfigValue(aiProperties.getProvider());
            
            return switch (activeProvider) {
                case OPENAI -> openaiAdapter.isHealthy();
                case OLLAMA -> ollamaAdapter.isHealthy();
                case NONE -> true;
            };
        } catch (Exception e) {
            log.error("Error getting active provider health", e);
            return false;
        }
    }

    /**
     * Get current AI provider type.
     * Useful for metrics, logging, and observability.
     */
    public String getActiveProvider() {
        return aiProperties.getProvider();
    }

    /**
     * Get diagnostic information about AI system state.
     * Used for troubleshooting and metrics.
     */
    public AiDiagnostics getDiagnostics() {
        return new AiDiagnostics(
            aiProperties.getProvider(),
            isHealthy,
            openaiAdapter.isHealthy(),
            ollamaAdapter.isHealthy()
        );
    }

    public record AiDiagnostics(
            String activeProvider,
            boolean overallHealth,
            boolean openaiHealthy,
            boolean ollamaHealthy) {
    }
}
