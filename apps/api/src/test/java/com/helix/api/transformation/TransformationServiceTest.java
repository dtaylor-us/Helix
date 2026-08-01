package com.helix.api.transformation;

import com.helix.api.onboarding.application.OnboardingService;
import com.helix.api.transformation.adapter.out.persistence.TransformationRepository;
import com.helix.api.transformation.application.TransformationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TransformationServiceTest {

    @Test
    void createWithGuidedFieldsPersistsDesiredIdentityAndObstacle() {
        var repository = Mockito.mock(TransformationRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var onboardingService = Mockito.mock(OnboardingService.class);
        var service = new TransformationService(repository, onboardingService);
        var transformation = service.create(
            "Become more peaceful in the face of criticism",
            "I want to stop losing days to a single hard conversation.",
            "Someone who can hear feedback without spiraling.",
            "I interpret feedback as a verdict on who I am, not on what I did."
        );

        assertEquals("Become more peaceful in the face of criticism", transformation.getTitle());
        assertEquals("Someone who can hear feedback without spiraling.", transformation.getDesiredIdentity());
        assertEquals("I interpret feedback as a verdict on who I am, not on what I did.", transformation.getObstacle());
        assertNotNull(transformation.getId());
        assertNotNull(transformation.getCreatedAt());
        Mockito.verify(onboardingService).advanceToFirstTransformationCreated();
    }

    @Test
    void createWithoutGuidedFieldsLeavesThemNull() {
        var repository = Mockito.mock(TransformationRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var onboardingService = Mockito.mock(OnboardingService.class);
        var service = new TransformationService(repository, onboardingService);
        var transformation = service.create("Build steadier habits", "Practice consistency without pressure");

        assertNull(transformation.getDesiredIdentity());
        assertNull(transformation.getObstacle());
    }
}
