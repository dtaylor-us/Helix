package com.helix.api.onboarding;

import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.onboarding.adapter.out.persistence.OnboardingStateRepository;
import com.helix.api.onboarding.application.OnboardingService;
import com.helix.api.onboarding.domain.OnboardingStateEntity;
import com.helix.api.onboarding.domain.OnboardingStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class OnboardingServiceTest {

    private static CurrentUserProvider stubCurrentUser(UUID ownerId) {
        var currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.currentUserId()).thenReturn(ownerId);
        return currentUserProvider;
    }

    @Test
    void getBootstrapsNotStartedWhenRowIsMissing() {
        var repository = Mockito.mock(OnboardingStateRepository.class);
        var ownerId = UUID.randomUUID();
        when(repository.findByOwnerId(ownerId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new OnboardingService(repository, stubCurrentUser(ownerId));
        var state = service.get();

        assertEquals(OnboardingStatus.NOT_STARTED, state.getStatus());
    }

    @Test
    void advanceToFirstTransformationCreatedMovesPastNotStarted() {
        var repository = Mockito.mock(OnboardingStateRepository.class);
        var ownerId = UUID.randomUUID();
        var existing = new OnboardingStateEntity(
            OnboardingStateEntity.SINGLETON_ID, OnboardingStatus.NOT_STARTED, OffsetDateTime.now().minusDays(1), ownerId
        );
        when(repository.findByOwnerId(ownerId)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new OnboardingService(repository, stubCurrentUser(ownerId));
        service.advanceToFirstTransformationCreated();

        assertEquals(OnboardingStatus.FIRST_TRANSFORMATION_CREATED, existing.getStatus());
    }

    @Test
    void advanceToCompleteIsNoOpWhenAlreadyComplete() {
        var repository = Mockito.mock(OnboardingStateRepository.class);
        var ownerId = UUID.randomUUID();
        var completedAt = OffsetDateTime.now().minusDays(1);
        var existing = new OnboardingStateEntity(OnboardingStateEntity.SINGLETON_ID, OnboardingStatus.COMPLETE, completedAt, ownerId);
        when(repository.findByOwnerId(ownerId)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new OnboardingService(repository, stubCurrentUser(ownerId));
        service.advanceToFirstTransformationCreated();

        assertEquals(OnboardingStatus.COMPLETE, existing.getStatus());
        assertEquals(completedAt, existing.getUpdatedAt());
    }

    @Test
    void advanceToCompleteMovesPastFirstTransformationCreated() {
        var repository = Mockito.mock(OnboardingStateRepository.class);
        var ownerId = UUID.randomUUID();
        var existing = new OnboardingStateEntity(
            OnboardingStateEntity.SINGLETON_ID, OnboardingStatus.FIRST_TRANSFORMATION_CREATED, OffsetDateTime.now().minusDays(1), ownerId
        );
        when(repository.findByOwnerId(ownerId)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new OnboardingService(repository, stubCurrentUser(ownerId));
        service.advanceToComplete();

        assertEquals(OnboardingStatus.COMPLETE, existing.getStatus());
    }
}
