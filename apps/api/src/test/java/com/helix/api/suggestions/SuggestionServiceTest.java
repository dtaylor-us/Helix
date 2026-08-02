package com.helix.api.suggestions;

import com.helix.api.experiments.application.ExperimentService;
import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.experiments.domain.ExperimentStatus;
import com.helix.api.suggestions.adapter.out.persistence.SuggestionRepository;
import com.helix.api.suggestions.application.SuggestionService;
import com.helix.api.suggestions.domain.SuggestionEntity;
import com.helix.api.suggestions.domain.SuggestionStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class SuggestionServiceTest {

    private final SuggestionRepository repository = Mockito.mock(SuggestionRepository.class);
    private final ExperimentService experimentService = Mockito.mock(ExperimentService.class);
    private final SuggestionService service = new SuggestionService(repository, experimentService);

    private SuggestionEntity proposedSuggestion(UUID experimentId, String text) {
        return new SuggestionEntity(UUID.randomUUID(), experimentId, null, text, SuggestionStatus.PROPOSED,
            null, OffsetDateTime.now(), null);
    }

    @Test
    void acceptingASuggestionRevisesTheExperimentsNextAction() {
        // Regression test: acceptance previously only flipped a status flag on the suggestion row,
        // with no visible effect on the experiment/journey (the UX gap this fix addresses).
        var experimentId = UUID.randomUUID();
        var suggestion = proposedSuggestion(experimentId, "Take one breath before responding");
        when(repository.findById(suggestion.getId())).thenReturn(Optional.of(suggestion));

        var result = service.accept(suggestion.getId());

        assertEquals(SuggestionStatus.ACCEPTED, result.getStatus());
        Mockito.verify(experimentService).reviseNextAction(experimentId, "Take one breath before responding");
    }

    @Test
    void replacingASuggestionRevisesTheExperimentsNextActionWithTheReplacementText() {
        var experimentId = UUID.randomUUID();
        var suggestion = proposedSuggestion(experimentId, "Original AI suggestion");
        when(repository.findById(suggestion.getId())).thenReturn(Optional.of(suggestion));

        var result = service.replace(suggestion.getId(), "My own smaller version");

        assertEquals(SuggestionStatus.REPLACED, result.getStatus());
        assertEquals("My own smaller version", result.getReplacementText());
        Mockito.verify(experimentService).reviseNextAction(experimentId, "My own smaller version");
    }

    @Test
    void dismissingASuggestionDoesNotTouchTheExperiment() {
        var experimentId = UUID.randomUUID();
        var suggestion = proposedSuggestion(experimentId, "Some suggestion");
        when(repository.findById(suggestion.getId())).thenReturn(Optional.of(suggestion));

        var result = service.dismiss(suggestion.getId());

        assertEquals(SuggestionStatus.DISMISSED, result.getStatus());
        Mockito.verifyNoInteractions(experimentService);
    }

    @Test
    void experimentServiceReviseNextActionPersistsTheChange() {
        // Direct coverage of the new ExperimentService method itself, independent of the suggestion
        // flow that calls it.
        var repository = Mockito.mock(com.helix.api.experiments.adapter.out.persistence.ExperimentRepository.class);
        var transformationService = Mockito.mock(com.helix.api.transformation.application.TransformationService.class);
        var aiAssistantPort = Mockito.mock(com.helix.api.ai.application.AiAssistantPort.class);
        var onboardingService = Mockito.mock(com.helix.api.onboarding.application.OnboardingService.class);
        var experimentService = new ExperimentService(repository, transformationService, aiAssistantPort, onboardingService);

        var experimentId = UUID.randomUUID();
        var experiment = new ExperimentEntity(experimentId, UUID.randomUUID(), "Pause before responding",
            "Pausing helps", "old next action", ExperimentStatus.ACTIVE, OffsetDateTime.now());
        when(repository.findById(experimentId)).thenReturn(Optional.of(experiment));
        when(repository.save(experiment)).thenReturn(experiment);

        var result = experimentService.reviseNextAction(experimentId, "Take one breath before responding");

        assertEquals("Take one breath before responding", result.getNextAction());
        Mockito.verify(repository).save(experiment);
    }
}
