package com.helix.api.beliefs;

import com.helix.api.beliefs.adapter.out.persistence.BeliefRepository;
import com.helix.api.beliefs.adapter.out.persistence.BeliefRevisionRepository;
import com.helix.api.beliefs.application.BeliefService;
import com.helix.api.beliefs.domain.BeliefEntity;
import com.helix.api.beliefs.domain.BeliefType;
import com.helix.api.transformation.application.TransformationService;
import com.helix.api.transformation.domain.TransformationEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BeliefServiceTest {

    @Test
    void reviseTracksPreviousAndNewBeliefState() {
        var repository = Mockito.mock(BeliefRepository.class);
        var revisionRepository = Mockito.mock(BeliefRevisionRepository.class);
        var transformationService = Mockito.mock(TransformationService.class);

        var transformationId = UUID.randomUUID();
        var beliefId = UUID.randomUUID();
        var belief = new BeliefEntity(
            beliefId,
            transformationId,
            "If I slow down, I will lose momentum.",
            BeliefType.LIMITING,
            OffsetDateTime.now().minusDays(3),
            OffsetDateTime.now().minusDays(3)
        );

        when(transformationService.get(transformationId)).thenReturn(new TransformationEntity(
            transformationId,
            "Build steadier habits",
            "Practice consistency without pressure",
            OffsetDateTime.now().minusDays(5)
        ));
        when(repository.findById(beliefId)).thenReturn(Optional.of(belief));
        when(revisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new BeliefService(repository, revisionRepository, transformationService);
        var revision = service.revise(
            beliefId,
            "If I slow down, I can notice what actually helps.",
            BeliefType.EMPOWERING,
            "Recent reflections showed steadier progress with smaller steps.",
            null
        );

        assertEquals("If I slow down, I will lose momentum.", revision.getPreviousStatement());
        assertEquals("If I slow down, I can notice what actually helps.", revision.getNewStatement());
        assertEquals(BeliefType.EMPOWERING, belief.getType());
        assertNotNull(belief.getRevisedAt());
    }
}