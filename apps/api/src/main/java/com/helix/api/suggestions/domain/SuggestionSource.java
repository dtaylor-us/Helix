package com.helix.api.suggestions.domain;

/**
 * Where a suggestion's text came from. See ADR-016: AI is the required content source for
 * post-reflection suggestions; DETERMINISTIC now means either the legacy template path
 * ({@link com.helix.api.suggestions.application.SuggestionService#createDeterministic}) or an
 * AI provider's circuit-breaker/no-provider fallback response, not a co-equal steady-state path.
 */
public enum SuggestionSource {
    AI,
    DETERMINISTIC
}
