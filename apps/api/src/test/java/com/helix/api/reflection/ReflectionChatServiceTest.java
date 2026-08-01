package com.helix.api.reflection;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.experiments.application.ExperimentService;
import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.experiments.domain.ExperimentStatus;
import com.helix.api.reflection.application.ReflectionChatService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

class ReflectionChatServiceTest {

    @Test
    void nextTurnBuildsTranscriptContextAndDoesNotPersistAnything() {
        var experimentService = Mockito.mock(ExperimentService.class);
        var aiAssistantPort = Mockito.mock(AiAssistantPort.class);

        var experimentId = UUID.randomUUID();
        when(experimentService.get(experimentId)).thenReturn(new ExperimentEntity(
            experimentId,
            UUID.randomUUID(),
            "Pause before responding",
            "Pausing helps me respond calmly",
            "Take one breath before replying",
            ExperimentStatus.ACTIVE,
            OffsetDateTime.now()
        ));
        when(aiAssistantPort.continueReflectionChat(any())).thenReturn(new AiAssistantPort.AiSuggestion(
            "What did you notice right after that moment?",
            "openai",
            "gpt-4o-mini",
            "v1",
            false
        ));

        var service = new ReflectionChatService(experimentService, aiAssistantPort);
        var response = service.nextTurn(experimentId, List.of(
            new ReflectionChatService.ChatMessage("user", "I paused once before replying."),
            new ReflectionChatService.ChatMessage("assistant", "What shifted after that pause?")
        ));

        assertEquals("What did you notice right after that moment?", response.text());
        Mockito.verify(aiAssistantPort).continueReflectionChat(argThat(context ->
            context.contains("Experiment: Pause before responding")
                && context.contains("Hypothesis: Pausing helps me respond calmly")
                && context.contains("Planned next action: Take one breath before replying")
                && context.contains("User: I paused once before replying.")
                && context.contains("Assistant: What shifted after that pause?")
        ));
        Mockito.verify(experimentService).get(experimentId);
        Mockito.verifyNoMoreInteractions(experimentService, aiAssistantPort);
    }

    @Test
    void finishBuildsTranscriptContextAndReturnsStructuredDraft() {
        var experimentService = Mockito.mock(ExperimentService.class);
        var aiAssistantPort = Mockito.mock(AiAssistantPort.class);

        var experimentId = UUID.randomUUID();
        when(experimentService.get(experimentId)).thenReturn(new ExperimentEntity(
            experimentId,
            UUID.randomUUID(),
            "Pause before responding",
            "Pausing helps me respond calmly",
            "Take one breath before replying",
            ExperimentStatus.ACTIVE,
            OffsetDateTime.now()
        ));
        when(aiAssistantPort.structureReflection(any())).thenReturn(new AiAssistantPort.AiReflectionStructure(
            "I paused before responding once and felt calmer afterward.",
            true,
            "My shoulders relaxed.",
            "The conversation stayed steady.",
            null,
            "openai",
            "gpt-4o-mini",
            false
        ));

        var service = new ReflectionChatService(experimentService, aiAssistantPort);
        var response = service.finish(experimentId, List.of(
            new ReflectionChatService.ChatMessage("user", "I paused before replying."),
            new ReflectionChatService.ChatMessage("assistant", "What did you notice in your body?")
        ));

        assertEquals("I paused before responding once and felt calmer afterward.", response.content());
        assertEquals(Boolean.TRUE, response.attempted());
        Mockito.verify(aiAssistantPort).structureReflection(argThat(context ->
            context.contains("Experiment: Pause before responding")
                && context.contains("User: I paused before replying.")
                && context.contains("Assistant: What did you notice in your body?")
        ));
        Mockito.verify(experimentService).get(experimentId);
        Mockito.verifyNoMoreInteractions(experimentService, aiAssistantPort);
    }
}
