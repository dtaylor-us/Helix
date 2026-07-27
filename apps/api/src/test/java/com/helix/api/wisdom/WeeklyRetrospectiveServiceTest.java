package com.helix.api.wisdom;

import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.wisdom.adapter.out.persistence.WeeklyRetrospectiveRepository;
import com.helix.api.wisdom.application.WeeklyRetrospectiveService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class WeeklyRetrospectiveServiceTest {

    @Test
    void draftContainsReflectionSummariesAndDeterministicAssistance() {
        var reflectionService = Mockito.mock(ReflectionService.class);
        var repository = Mockito.mock(WeeklyRetrospectiveRepository.class);

        when(reflectionService.recentSince(any())).thenReturn(List.of(
            new ReflectionEntity(UUID.randomUUID(), UUID.randomUUID(),
                "I completed the smaller version of the experiment and felt more consistent.",
                OffsetDateTime.now().minusDays(1))
        ));

        var service = new WeeklyRetrospectiveService(reflectionService, repository);
        var draft = service.draft();

        assertFalse(draft.reflectionSummaries().isEmpty());
        assertTrue(draft.assistance().contains("recurring pattern"));
    }
}
