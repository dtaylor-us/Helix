package com.helix.api.reflection;

import com.helix.api.experiments.application.ExperimentService;
import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.experiments.domain.ExperimentStatus;
import com.helix.api.reflection.adapter.out.persistence.ReflectionRepository;
import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.suggestions.application.SuggestionService;
import com.helix.api.suggestions.domain.SuggestionEntity;
import com.helix.api.suggestions.domain.SuggestionStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

class ReflectionServiceTest {

    @Test
    void createAddsDeterministicSuggestion() {
        var repo = Mockito.mock(ReflectionRepository.class);
        var experimentService = Mockito.mock(ExperimentService.class);
        var suggestionService = Mockito.mock(SuggestionService.class);

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

        when(suggestionService.createDeterministic(any(), any(), any(), anyInt())).thenReturn(new SuggestionEntity(
            UUID.randomUUID(),
            experimentId,
            reflectionId,
            "Optional next step: Walk after breakfast",
            SuggestionStatus.PROPOSED,
            null,
            OffsetDateTime.now(),
            null
        ));

        var service = new ReflectionService(repo, experimentService, suggestionService);
        var result = service.create(experimentId, "I did half of it and felt better.");

        assertTrue(result.suggestion().getText().startsWith("Optional next step:"));
    }
}
