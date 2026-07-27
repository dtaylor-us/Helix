package com.helix.api.ai.adapter.out;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.ai.config.AiProperties;
import com.helix.api.ai.config.AiProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI Provider Factory - selects and instantiates the appropriate AI adapter.
 * 
 * Governance:
 * - ADR-006: AI is optional; factory enables seamless switching between adapters
 * - ADR-007: Local-first by design; Ollama supported alongside OpenAI
 * - ADR-008: User control via configuration; provider selection is explicit and auditable
 * 
 * Default provider: OpenAI (configurable via helix.ai.provider)
 * Fallback: NoAiAssistantAdapter (deterministic, always available)
 * 
 * Configuration (application.yml):
 *   helix:
 *     ai:
 *       provider: openai           # options: openai (default), ollama, none
 *       openai:
 *         api-key: ${OPENAI_API_KEY}
 *       ollama:
 *         base-url: http://localhost:11434
 */
@Configuration
public class AiProviderFactory {
    
    private static final Logger log = LoggerFactory.getLogger(AiProviderFactory.class);

    /**
     * Factory method: creates the appropriate AiAssistantPort bean based on configuration.
     * 
     * Selection logic:
     * 1. Read helix.ai.provider config (default: "openai")
     * 2. Instantiate corresponding adapter
     * 3. Validate required credentials (e.g., OPENAI_API_KEY for OpenAI)
     * 4. Fall back to NoOp if provider fails validation
     */
    @Bean
    public AiAssistantPort aiAssistantPort(
            AiProperties aiProperties,
            OpenAiAssistantAdapter openaiAdapter,
            OllamaAssistantAdapter ollamaAdapter,
            NoAiAssistantAdapter noOpAdapter) {
        
        AiProviderType providerType = AiProviderType.fromConfigValue(aiProperties.getProvider());
        
        AiAssistantPort selectedAdapter = switch (providerType) {
            case OPENAI -> {
                if (isOpenAiConfigValid(aiProperties)) {
                    log.info("AI Provider initialized: OpenAI (model: {})", 
                        aiProperties.getOpenai().getModel());
                    yield openaiAdapter;
                } else {
                    log.warn("OpenAI configuration invalid (missing API key). Falling back to NoOp adapter.");
                    yield noOpAdapter;
                }
            }
            case OLLAMA -> {
                log.info("AI Provider initialized: Ollama (base URL: {}, model: {})",
                    aiProperties.getOllama().getBaseUrl(),
                    aiProperties.getOllama().getModel());
                yield ollamaAdapter;
            }
            case NONE -> {
                log.info("AI Provider explicitly disabled (NONE). Using deterministic fallback.");
                yield noOpAdapter;
            }
        };
        
        return selectedAdapter;
    }

    /**
     * Validates OpenAI configuration is sufficient to attempt connections.
     */
    private boolean isOpenAiConfigValid(AiProperties aiProperties) {
        String apiKey = aiProperties.getOpenai().getApiKey();
        return apiKey != null && !apiKey.isEmpty() && !apiKey.startsWith("${");
    }
}
