package com.helix.api.onboarding;

import com.helix.api.onboarding.adapter.out.persistence.OnboardingStateRepository;
import com.helix.api.onboarding.application.OnboardingService;
import com.helix.api.onboarding.domain.OnboardingStateEntity;
import com.helix.api.onboarding.domain.OnboardingStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OnboardingServiceTest {

    @Test
    void getBootstrapsNotStartedWhenSingletonRowIsMissing() {
        var repository = Mockito.mock(OnboardingStateRepository.class);
        when(repository.findById(OnboardingStateEntity.SINGLETON_ID)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new OnboardingService(repository);
        var state = service.get();

        assertEquals(OnboardingStatus.NOT_STARTED, state.getStatus());
    }

    @Test
    void advanceToFirstTransformationCreatedMovesPastNotStarted() {
        var repository = Mockito.mock(OnboardingStateRepository.class);
        var existing = new OnboardingStateEntity(
            OnboardingStateEntity.SINGLETON_ID, OnboardingStatus.NOT_STARTED, OffsetDateTime.now().minusDays(1)
        );
        when(repository.findById(OnboardingStateEntity.SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new OnboardingService(repository);
        service.advanceToFirstTransformationCreated();

        assertEquals(OnboardingStatus.FIRST_TRANSFORMATION_CREATED, existing.getStatus());
    }

    @Test
    void advanceToCompleteIsNoOpWhenAlreadyComplete() {
        var repository = Mockito.mock(OnboardingStateRepository.class);
        var completedAt = OffsetDateTime.now().minusDays(1);
        var existing = new OnboardingStateEntity(OnboardingStateEntity.SINGLETON_ID, OnboardingStatus.COMPLETE, completedAt);
        when(repository.findById(OnboardingStateEntity.SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new OnboardingService(repository);
        service.advanceToFirstTransformationCreated();

        assertEquals(OnboardingStatus.COMPLETE, existing.getStatus());
        assertEquals(completedAt, existing.getUpdatedAt());
    }

    @Test
    void advanceToCompleteMovesPastFirstTransformationCreated() {
        var repository = Mockito.mock(OnboardingStateRepository.class);
        var existing = new OnboardingStateEntity(
            OnboardingStateEntity.SINGLETON_ID, OnboardingStatus.FIRST_TRANSFORMATION_CREATED, OffsetDateTime.now().minusDays(1)
        );
        when(repository.findById(OnboardingStateEntity.SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new OnboardingService(repository);
        service.advanceToComplete();

        assertEquals(OnboardingStatus.COMPLETE, existing.getStatus());
    }
}
