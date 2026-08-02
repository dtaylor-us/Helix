package com.helix.api.experiments;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.experiments.adapter.out.persistence.ExperimentRepository;
import com.helix.api.experiments.application.ExperimentService;
import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.onboarding.application.OnboardingService;
import com.helix.api.transformation.application.TransformationService;
import com.helix.api.transformation.domain.TransformationEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ExperimentServiceTest {

    private static CurrentUserProvider stubCurrentUser() {
        var currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.currentUserId()).thenReturn(UUID.randomUUID());
        return currentUserProvider;
    }

    @Test
    void createWithGuidedFieldsPersistsCadenceEvidenceAndReviewDate() {
        var repository = Mockito.mock(ExperimentRepository.class);
        var transformationService = Mockito.mock(TransformationService.class);

        var transformationId = UUID.randomUUID();
        when(transformationService.get(transformationId)).thenReturn(new TransformationEntity(
            transformationId, "Become more peaceful", "Practice steadiness", OffsetDateTime.now().minusDays(1)
        ));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var aiAssistantPort = Mockito.mock(AiAssistantPort.class);
        var onboardingService = Mockito.mock(OnboardingService.class);
        var service = new ExperimentService(repository, transformationService, aiAssistantPort, onboardingService, stubCurrentUser());
        var reviewDate = LocalDate.now().plusWeeks(1);
        var experiment = service.create(
            transformationId,
            "Pause before responding",
            "Pausing helps me respond calmly",
            "Take one breath before replying",
            "Whenever I feel criticized",
            "Fewer moments of regretting how I responded",
            reviewDate
        );

        assertEquals("Whenever I feel criticized", experiment.getCadence());
        assertEquals("Fewer moments of regretting how I responded", experiment.getEvidenceOfSuccess());
        assertEquals(reviewDate, experiment.getReviewAt());
        Mockito.verify(onboardingService).advanceToComplete();
    }

    @Test
    void createWithoutGuidedFieldsLeavesThemNull() {
        var repository = Mockito.mock(ExperimentRepository.class);
        var transformationService = Mockito.mock(TransformationService.class);

        var transformationId = UUID.randomUUID();
        when(transformationService.get(transformationId)).thenReturn(new TransformationEntity(
            transformationId, "Become more peaceful", "Practice steadiness", OffsetDateTime.now().minusDays(1)
        ));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var aiAssistantPort = Mockito.mock(AiAssistantPort.class);
        var onboardingService = Mockito.mock(OnboardingService.class);
        var service = new ExperimentService(repository, transformationService, aiAssistantPort, onboardingService, stubCurrentUser());
        var experiment = service.create(transformationId, "Pause before responding", "Pausing helps", "Breathe once");

        assertNull(experiment.getCadence());
        assertNull(experiment.getEvidenceOfSuccess());
        assertNull(experiment.getReviewAt());
    }

    @Test
    void proposeDraftReturnsAiDraftWithoutPersistingAnything() {
        var repository = Mockito.mock(ExperimentRepository.class);
        var transformationService = Mockito.mock(TransformationService.class);
        var aiAssistantPort = Mockito.mock(AiAssistantPort.class);

        var transformationId = UUID.randomUUID();
        when(transformationService.get(transformationId)).thenReturn(new TransformationEntity(
            transformationId, "Become more peaceful", "Practice steadiness", OffsetDateTime.now().minusDays(1)
        ));
        when(aiAssistantPort.proposeExperiment(any())).thenReturn(new AiAssistantPort.AiExperimentDraft(
            "Pause before responding", "If I pause, I respond more calmly", "Take one breath before replying",
            "Whenever I feel criticized", "Fewer moments of regret", "openai", "gpt-4o-mini", false
        ));

        var onboardingService = Mockito.mock(OnboardingService.class);
        var service = new ExperimentService(repository, transformationService, aiAssistantPort, onboardingService, stubCurrentUser());
        var draft = service.proposeDraft(transformationId);

        assertEquals("Pause before responding", draft.title());
        assertEquals("AI", draft.source());
        Mockito.verifyNoInteractions(repository);
        Mockito.verifyNoInteractions(onboardingService);
    }
}
