package com.helix.api.ai.config;

/**
 * Supported AI provider types for Helix.
 * 
 * Governed by ADR-006 (optional AI) and ADR-007 (local-first with cloud support).
 * Each provider must implement AiAssistantPort and support graceful fallback.
 */
public enum AiProviderType {
    /** OpenAI (default): cloud-based LLM service */
    OPENAI("openai"),
    
    /** Local Ollama instance: on-device inference */
    OLLAMA("ollama"),
    
    /** No-op adapter: deterministic fallback for testing and offline use */
    NONE("none");

    public final String configValue;

    AiProviderType(String configValue) {
        this.configValue = configValue;
    }

    public static AiProviderType fromConfigValue(String value) {
        for (AiProviderType type : AiProviderType.values()) {
            if (type.configValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown AI provider type: " + value);
    }
}
