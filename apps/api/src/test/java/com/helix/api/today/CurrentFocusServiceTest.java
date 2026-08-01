package com.helix.api.today;

import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.experiments.domain.ExperimentStatus;
import com.helix.api.onboarding.application.OnboardingService;
import com.helix.api.onboarding.domain.OnboardingStateEntity;
import com.helix.api.onboarding.domain.OnboardingStatus;
import com.helix.api.today.application.CurrentFocusService;
import com.helix.api.today.application.TodayService;
import com.helix.api.transformation.application.TransformationService;
import com.helix.api.transformation.domain.TransformationEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class CurrentFocusServiceTest {

    @Test
    void snapshotReflectsNotStartedOnboardingWithNoTransformationsAndNoActiveExperiment() {
        var todayService = Mockito.mock(TodayService.class);
        var transformationService = Mockito.mock(TransformationService.class);
        var onboardingService = Mockito.mock(OnboardingService.class);

        when(onboardingService.get()).thenReturn(
            new OnboardingStateEntity(OnboardingStateEntity.SINGLETON_ID, OnboardingStatus.NOT_STARTED, OffsetDateTime.now())
        );
        when(transformationService.list()).thenReturn(List.of());
        when(todayService.snapshot()).thenReturn(Optional.empty());

        var service = new CurrentFocusService(todayService, transformationService, onboardingService);
        var snapshot = service.snapshot();

        assertEquals(OnboardingStatus.NOT_STARTED, snapshot.onboardingStatus());
        assertTrue(snapshot.transformations().isEmpty());
        assertNull(snapshot.today());
    }

    @Test
    void snapshotIncludesTransformationsAndActiveExperimentWhenPresent() {
        var todayService = Mockito.mock(TodayService.class);
        var transformationService = Mockito.mock(TransformationService.class);
        var onboardingService = Mockito.mock(OnboardingService.class);

        when(onboardingService.get()).thenReturn(
            new OnboardingStateEntity(OnboardingStateEntity.SINGLETON_ID, OnboardingStatus.COMPLETE, OffsetDateTime.now())
        );
        var transformationId = UUID.randomUUID();
        when(transformationService.list()).thenReturn(List.of(
            new TransformationEntity(transformationId, "Become more peaceful", "Practice steadiness", OffsetDateTime.now())
        ));
        var experimentId = UUID.randomUUID();
        var experiment = new ExperimentEntity(
            experimentId, transformationId, "Pause before responding", "Pausing helps",
            "Breathe once", ExperimentStatus.ACTIVE, OffsetDateTime.now()
        );
        when(todayService.snapshot()).thenReturn(Optional.of(new TodayService.TodaySnapshot(experiment, List.of(), List.of())));

        var service = new CurrentFocusService(todayService, transformationService, onboardingService);
        var snapshot = service.snapshot();

        assertEquals(OnboardingStatus.COMPLETE, snapshot.onboardingStatus());
        assertEquals(1, snapshot.transformations().size());
        assertEquals(experimentId, snapshot.today().activeExperiment().getId());
    }
}
