package com.helix.api.reflection;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.experiments.application.ExperimentService;
import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.experiments.domain.ExperimentStatus;
import com.helix.api.reflection.adapter.out.persistence.ReflectionRepository;
import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.suggestions.application.SuggestionService;
import com.helix.api.suggestions.domain.SuggestionEntity;
import com.helix.api.suggestions.domain.SuggestionSource;
import com.helix.api.suggestions.domain.SuggestionStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ReflectionServiceTest {

    @Test
    void createAddsAiGeneratedSuggestion() {
        var repo = Mockito.mock(ReflectionRepository.class);
        var experimentService = Mockito.mock(ExperimentService.class);
        var suggestionService = Mockito.mock(SuggestionService.class);
        var aiAssistantPort = Mockito.mock(AiAssistantPort.class);

        var experimentId = UUID.randomUUID();
        var reflectionId = UUID.randomUUID();

        when(experimentService.get(experimentId)).thenReturn(new ExperimentEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Morning walk",
            "If I walk for 10 minutes I will feel calmer",
            "Walk after breakfast",
            ExperimentStatus.ACTIVE,
            OffsetDateTime.now()
        ));

        when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repo.findByExperimentIdOrderByCreatedAtDesc(experimentId)).thenReturn(List.of());

        when(aiAssistantPort.suggestNextAction(any())).thenReturn(new AiAssistantPort.AiSuggestion(
            "Walk after breakfast again tomorrow.", "openai", "gpt-4o-mini", "v1", false
        ));

        when(suggestionService.createFromAi(any(), any(), anyString(), anyString(), anyString(), anyBoolean())).thenReturn(
            new SuggestionEntity(
                UUID.randomUUID(),
                experimentId,
                reflectionId,
                "Walk after breakfast again tomorrow.",
                SuggestionStatus.PROPOSED,
                null,
                OffsetDateTime.now(),
                null,
                SuggestionSource.AI,
                "openai",
                "gpt-4o-mini"
            )
        );

        var service = new ReflectionService(repo, experimentService, suggestionService, aiAssistantPort);
        var result = service.create(experimentId, "I did half of it and felt better.");

        assertTrue(result.suggestion().getText().startsWith("Walk after breakfast"));
        assertEquals(SuggestionSource.AI, result.suggestion().getSource());
        assertEquals(null, result.reflection().getAttempted());
    }

    @Test
    void createWithProgressiveAnswersPersistsThem() {
        var repo = Mockito.mock(ReflectionRepository.class);
        var experimentService = Mockito.mock(ExperimentService.class);
        var suggestionService = Mockito.mock(SuggestionService.class);
        var aiAssistantPort = Mockito.mock(AiAssistantPort.class);

        var experimentId = UUID.randomUUID();
        var reflectionId = UUID.randomUUID();

        when(experimentService.get(experimentId)).thenReturn(new ExperimentEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Pause before responding",
            "Pausing helps me respond calmly",
            "Take one breath before replying",
            ExperimentStatus.ACTIVE,
            OffsetDateTime.now()
        ));

        when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repo.findByExperimentIdOrderByCreatedAtDesc(experimentId)).thenReturn(List.of());

        when(aiAssistantPort.suggestNextAction(any())).thenReturn(new AiAssistantPort.AiSuggestion(
            "Take one breath before replying again today.", "openai", "gpt-4o-mini", "v1", false
        ));

        when(suggestionService.createFromAi(any(), any(), anyString(), anyString(), anyString(), anyBoolean())).thenReturn(
            new SuggestionEntity(
                UUID.randomUUID(),
                experimentId,
                reflectionId,
                "Take one breath before replying again today.",
                SuggestionStatus.PROPOSED,
                null,
                OffsetDateTime.now(),
                null,
                SuggestionSource.AI,
                "openai",
                "gpt-4o-mini"
            )
        );

        var service = new ReflectionService(repo, experimentService, suggestionService, aiAssistantPort);
        var result = service.create(
            experimentId,
            "I paused twice today.",
            true,
            "My shoulders were tense before I paused.",
            "The conversation stayed calmer than usual.",
            "I didn't expect it to feel this natural by the second try."
        );

        assertEquals(Boolean.TRUE, result.reflection().getAttempted());
        assertEquals("My shoulders were tense before I paused.", result.reflection().getNoticed());
        assertEquals("The conversation stayed calmer than usual.", result.reflection().getEvidenceNoted());
        assertEquals("I didn't expect it to feel this natural by the second try.", result.reflection().getSurprise());
    }
}
