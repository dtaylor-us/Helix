package com.helix.api.suggestions.application;

import com.helix.api.experiments.application.ExperimentService;
import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.suggestions.adapter.out.persistence.SuggestionRepository;
import com.helix.api.suggestions.domain.SuggestionEntity;
import com.helix.api.suggestions.domain.SuggestionSource;
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
    private final ExperimentService experimentService;
    private final CurrentUserProvider currentUserProvider;

    public SuggestionService(
        SuggestionRepository repository, ExperimentService experimentService, CurrentUserProvider currentUserProvider
    ) {
        this.repository = repository;
        this.experimentService = experimentService;
        this.currentUserProvider = currentUserProvider;
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
            null,
            SuggestionSource.DETERMINISTIC,
            null,
            null,
            currentUserProvider.currentUserId()
        );
        return repository.save(suggestion);
    }

    /**
     * Create a suggestion from an AI-generated proposal (see ADR-016). {@code deterministicFallback}
     * is true when the AI adapter itself fell back to a canned response (provider outage, no
     * provider configured); in that case the suggestion is recorded as DETERMINISTIC even though it
     * came from the AI call path, so provenance accurately reflects what the user actually saw.
     */
    public SuggestionEntity createFromAi(
        UUID experimentId, UUID reflectionId, String text, String provider, String model, boolean deterministicFallback
    ) {
        var suggestion = new SuggestionEntity(
            UUID.randomUUID(),
            experimentId,
            reflectionId,
            text,
            SuggestionStatus.PROPOSED,
            null,
            OffsetDateTime.now(),
            null,
            deterministicFallback ? SuggestionSource.DETERMINISTIC : SuggestionSource.AI,
            provider,
            model,
            currentUserProvider.currentUserId()
        );
        return repository.save(suggestion);
    }

    public List<SuggestionEntity> history(UUID experimentId) {
        return repository.findByExperimentIdOrderByCreatedAtDesc(experimentId);
    }

    /**
     * Accepting commits the user to this action, so it now also becomes the experiment's actual
     * next action ({@link ExperimentService#reviseNextAction}) -- previously acceptance only flipped
     * a status flag on the suggestion row itself, with no visible effect anywhere else in the app
     * (not on the experiment, not on any journey-facing page).
     */
    @Transactional
    public SuggestionEntity accept(UUID id) {
        var suggestion = get(id);
        suggestion.accept();
        experimentService.reviseNextAction(suggestion.getExperimentId(), suggestion.getText());
        return suggestion;
    }

    @Transactional
    public SuggestionEntity dismiss(UUID id) {
        var suggestion = get(id);
        suggestion.dismiss();
        return suggestion;
    }

    /**
     * Replacing is itself a form of acceptance -- the user is committing to their own smaller
     * version of the action instead of the proposed one -- so it updates the experiment's next
     * action the same way {@link #accept} does, using the user's replacement text.
     */
    @Transactional
    public SuggestionEntity replace(UUID id, String replacementText) {
        var suggestion = get(id);
        suggestion.replaceWith(replacementText);
        experimentService.reviseNextAction(suggestion.getExperimentId(), replacementText);
        return suggestion;
    }

    public SuggestionEntity get(UUID id) {
        return repository.findByIdAndOwnerId(id, currentUserProvider.currentUserId())
            .orElseThrow(() -> new NoSuchElementException("Suggestion not found"));
    }
}
