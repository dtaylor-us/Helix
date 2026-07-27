package com.helix.api.suggestions.application;

import com.helix.api.suggestions.adapter.out.persistence.SuggestionRepository;
import com.helix.api.suggestions.domain.SuggestionEntity;
import com.helix.api.suggestions.domain.SuggestionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class SuggestionService {

    private final SuggestionRepository repository;

    public SuggestionService(SuggestionRepository repository) {
        this.repository = repository;
    }

    public SuggestionEntity createDeterministic(UUID experimentId, UUID reflectionId, String nextAction, int previousAttempts) {
        String text;
        if (nextAction != null && !nextAction.isBlank()) {
            text = "Optional next step: " + nextAction.trim();
        } else if (previousAttempts > 1) {
            text = "Optional next step: reduce your experiment to a 10-minute action and try again today.";
        } else {
            text = "Optional next step: write one sentence about what made this experiment easier or harder today.";
        }
        var suggestion = new SuggestionEntity(
            UUID.randomUUID(),
            experimentId,
            reflectionId,
            text,
            SuggestionStatus.PROPOSED,
            null,
            OffsetDateTime.now(),
            null
        );
        return repository.save(suggestion);
    }

    public List<SuggestionEntity> history(UUID experimentId) {
        return repository.findByExperimentIdOrderByCreatedAtDesc(experimentId);
    }

    @Transactional
    public SuggestionEntity accept(UUID id) {
        var suggestion = get(id);
        suggestion.accept();
        return suggestion;
    }

    @Transactional
    public SuggestionEntity dismiss(UUID id) {
        var suggestion = get(id);
        suggestion.dismiss();
        return suggestion;
    }

    @Transactional
    public SuggestionEntity replace(UUID id, String replacementText) {
        var suggestion = get(id);
        suggestion.replaceWith(replacementText);
        return suggestion;
    }

    public SuggestionEntity get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Suggestion not found"));
    }
}
