package com.helix.api.wisdom.domain;

/**
 * Where a weekly retrospective's summary/assistance text came from. See ADR-016: AI is the
 * required content source for the weekly retrospective narrative; DETERMINISTIC now means either
 * the legacy count/length-based templating or an AI provider's circuit-breaker/no-provider
 * fallback response, not a co-equal steady-state path.
 *
 * Mirrors {@code com.helix.api.suggestions.domain.SuggestionSource} but kept as its own type
 * rather than shared, so the wisdom module doesn't take a domain-level dependency on the
 * suggestions module (each feature module owns its own domain vocabulary per ADR-001).
 */
public enum RetrospectiveSource {
    AI,
    DETERMINISTIC
}
