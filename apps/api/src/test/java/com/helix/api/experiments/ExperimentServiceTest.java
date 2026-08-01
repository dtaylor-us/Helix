package com.helix.api.experiments;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.experiments.adapter.out.persistence.ExperimentRepository;
import com.helix.api.experiments.application.ExperimentService;
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

    @Test
    void createWithGuidedFieldsPersistsCadenceEvidenceAndReviewDate() {
        var repository = Mockito.mock(ExperimentRepository.class);
        var transformationService = Mockito.mock(TransformationService.class);
        var aiAssistantPort = Mockito.mock(AiAssistantPort.class);

        var transformationId = UUID.randomUUID();
        when(transformationService.get(transformationId)).thenReturn(new TransformationEntity(
            transformationId, "Become more peaceful", "Practice steadiness", OffsetDateTime.now().minusDays(1)
        ));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new ExperimentService(repository, transformationService, aiAssistantPort);
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
    }

    @Test
    void createWithoutGuidedFieldsLeavesThemNull() {
        var repository = Mockito.mock(ExperimentRepository.class);
        var transformationService = Mockito.mock(TransformationService.class);
        var aiAssistantPort = Mockito.mock(AiAssistantPort.class);

        var transformationId = UUID.randomUUID();
        when(transformationService.get(transformationId)).thenReturn(new TransformationEntity(
            transformationId, "Become more peaceful", "Practice steadiness", OffsetDateTime.now().minusDays(1)
        ));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new ExperimentService(repository, transformationService, aiAssistantPort);
        var experiment = service.create(transformationId, "Pause before responding", "Pausing helps", "Breathe once");

        assertNull(experiment.getCadence());
        assertNull(experiment.getEvidenceOfSuccess());
        assertNull(experiment.getReviewAt());
    }

    @Test
    void proposeDraftMapsFallbackProvenanceAndCoreFields() {
        var repository = Mockito.mock(ExperimentRepository.class);
        var transformationService = Mockito.mock(TransformationService.class);
        var aiAssistantPort = Mockito.mock(AiAssistantPort.class);

        var transformationId = UUID.randomUUID();
        when(transformationService.get(transformationId)).thenReturn(new TransformationEntity(
            transformationId,
            "Become more peaceful",
            "Practice steadiness",
            "Respond calmly",
            "Feeling rushed",
            OffsetDateTime.now().minusDays(1)
        ));
        when(aiAssistantPort.proposeExperiment(any())).thenReturn(new AiAssistantPort.AiExperimentDraft(
            "First small step toward peace",
            "A smaller pause will help me learn what steadiness feels like.",
            "Take one breath before replying once today.",
            "Once today",
            "I notice one calmer response.",
            "none",
            "deterministic",
            "v1",
            true
        ));

        var service = new ExperimentService(repository, transformationService, aiAssistantPort);
        var draft = service.proposeDraft(transformationId);

        assertEquals("First small step toward peace", draft.title());
        assertEquals("A smaller pause will help me learn what steadiness feels like.", draft.hypothesis());
        assertEquals("Take one breath before replying once today.", draft.nextAction());
        assertEquals("DETERMINISTIC", draft.source());
        assertEquals("none", draft.aiProvider());
        assertEquals("deterministic", draft.aiModel());
    }
}
