package com.helix.api.suggestions;

import com.helix.api.suggestions.domain.SuggestionEntity;
import com.helix.api.suggestions.domain.SuggestionSource;
import com.helix.api.suggestions.domain.SuggestionStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SuggestionEntityTest {

    @Test
    void replaceTransitionUpdatesState() {
        var suggestion = new SuggestionEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Original",
            SuggestionStatus.PROPOSED,
            null,
            OffsetDateTime.now(),
            null
        );

        suggestion.replaceWith("Smaller action");

        assertEquals(SuggestionStatus.REPLACED, suggestion.getStatus());
        assertEquals("Smaller action", suggestion.getReplacementText());
    }

    @Test
    void legacyConstructorDefaultsToDeterministicSourceWithNoProvenance() {
        var suggestion = new SuggestionEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Original",
            SuggestionStatus.PROPOSED,
            null,
            OffsetDateTime.now(),
            null
        );

        assertEquals(SuggestionSource.DETERMINISTIC, suggestion.getSource());
        assertNull(suggestion.getAiProvider());
        assertNull(suggestion.getAiModel());
    }

    @Test
    void fullConstructorRecordsAiProvenance() {
        var suggestion = new SuggestionEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Walk after breakfast again tomorrow.",
            SuggestionStatus.PROPOSED,
            null,
            OffsetDateTime.now(),
            null,
            SuggestionSource.AI,
            "openai",
            "gpt-4o-mini"
        );

        assertEquals(SuggestionSource.AI, suggestion.getSource());
        assertEquals("openai", suggestion.getAiProvider());
        assertEquals("gpt-4o-mini", suggestion.getAiModel());
    }
}
