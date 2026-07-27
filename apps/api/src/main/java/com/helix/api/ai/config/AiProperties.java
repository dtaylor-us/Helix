package com.helix.api.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI provider configuration properties.
 * 
 * Read from application.yml or application.properties:
 * 
 * helix:
 *   ai:
 *     provider: openai                    # default: openai, options: openai|ollama|none
 *     timeout-seconds: 10                 # default: 10
 *     retry-max-attempts: 3               # default: 3
 *     retry-delay-ms: 100                 # default: 100
 *     openai:
 *       api-key: ${OPENAI_API_KEY}       # required if provider=openai
 *       model: gpt-4o-mini               # default: gpt-4o-mini
 *       base-url: https://api.openai.com # default: https://api.openai.com
 *     ollama:
 *       base-url: http://localhost:11434 # default: http://localhost:11434
 *       model: llama2                     # default: llama2
 * 
 * Governed by ADR-007 (local-first approach) and ADR-008 (user governance).
 */
@Component
@ConfigurationProperties(prefix = "helix.ai")
public class AiProperties {
    
    /** Active AI provider (default: openai) */
    private String provider = "openai";
    
    /** Request timeout in seconds (default: 10) */
    private int timeoutSeconds = 10;
    
    /** Maximum retry attempts (default: 3) */
    private int retryMaxAttempts = 3;
    
    /** Initial retry delay in milliseconds (default: 100) */
    private int retryDelayMs = 100;
    
    /** OpenAI-specific configuration */
    private OpenAiConfig openai = new OpenAiConfig();
    
    /** Ollama-specific configuration */
    private OllamaConfig ollama = new OllamaConfig();
    
    public static class OpenAiConfig {
        /** OpenAI API key (required if provider=openai) */
        private String apiKey;
        
        /** Model name (default: gpt-4o-mini) */
        private String model = "gpt-4o-mini";
        
        /** Base URL for OpenAI API (default: https://api.openai.com) */
        private String baseUrl = "https://api.openai.com";
        
        /** Temperature for response generation (default: 0.7) */
        private double temperature = 0.7;
        
        /** Max tokens per response (default: 150) */
        private int maxTokens = 150;

        // Getters and setters
        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
    }
    
    public static class OllamaConfig {
        /** Base URL for Ollama service (default: http://localhost:11434) */
        private String baseUrl = "http://localhost:11434";
        
        /** Model name (default: llama2) */
        private String model = "llama2";
        
        /** Temperature for response generation (default: 0.7) */
        private double temperature = 0.7;

        // Getters and setters
        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
    }
    
    // Getters and setters
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getRetryMaxAttempts() {
        return retryMaxAttempts;
    }

    public void setRetryMaxAttempts(int retryMaxAttempts) {
        this.retryMaxAttempts = retryMaxAttempts;
    }

    public int getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(int retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }

    public OpenAiConfig getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAiConfig openai) {
        this.openai = openai;
    }

    public OllamaConfig getOllama() {
        return ollama;
    }

    public void setOllama(OllamaConfig ollama) {
        this.ollama = ollama;
    }
}
