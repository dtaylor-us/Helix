package com.helix.api.suggestions;

import com.helix.api.suggestions.domain.SuggestionEntity;
import com.helix.api.suggestions.domain.SuggestionStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
