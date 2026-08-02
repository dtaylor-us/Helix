package com.helix.api.wisdom;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.wisdom.adapter.out.persistence.WeeklyRetrospectiveRepository;
import com.helix.api.wisdom.application.WeeklyRetrospectiveService;
import com.helix.api.wisdom.domain.RetrospectiveSource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class WeeklyRetrospectiveServiceTest {

    @Test
    void draftContainsReflectionSummariesAndAiAssistance() {
        var reflectionService = Mockito.mock(ReflectionService.class);
        var repository = Mockito.mock(WeeklyRetrospectiveRepository.class);
        var aiAssistantPort = Mockito.mock(AiAssistantPort.class);

        when(reflectionService.recentSince(any())).thenReturn(List.of(
            new ReflectionEntity(UUID.randomUUID(), UUID.randomUUID(),
                "I completed the smaller version of the experiment and felt more consistent.",
                OffsetDateTime.now().minusDays(1))
        ));

        when(aiAssistantPort.summarizeWeek(any())).thenReturn(new AiAssistantPort.AiWeeklySummary(
            "You leaned into smaller, steadier versions of the experiment this week.",
            "Choose one recurring pattern and run a smaller experiment next week.",
            "openai", "gpt-4o-mini", false
        ));

        var currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.currentUserId()).thenReturn(UUID.randomUUID());
        var service = new WeeklyRetrospectiveService(reflectionService, repository, aiAssistantPort, currentUserProvider);
        var draft = service.draft();

        assertFalse(draft.reflectionSummaries().isEmpty());
        assertTrue(draft.assistance().contains("recurring pattern"));
        assertEquals(RetrospectiveSource.AI, draft.source());
    }

    @Test
    void draftSkipsAiWhenThereAreNoReflectionsThisWeek() {
        var reflectionService = Mockito.mock(ReflectionService.class);
        var repository = Mockito.mock(WeeklyRetrospectiveRepository.class);
        var aiAssistantPort = Mockito.mock(AiAssistantPort.class);

        when(reflectionService.recentSince(any())).thenReturn(List.of());

        var currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.currentUserId()).thenReturn(UUID.randomUUID());
        var service = new WeeklyRetrospectiveService(reflectionService, repository, aiAssistantPort, currentUserProvider);
        var draft = service.draft();

        assertTrue(draft.reflectionSummaries().isEmpty());
        assertEquals(RetrospectiveSource.DETERMINISTIC, draft.source());
        Mockito.verifyNoInteractions(aiAssistantPort);
    }
}
