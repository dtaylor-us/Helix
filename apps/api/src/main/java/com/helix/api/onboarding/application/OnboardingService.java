package com.helix.api.onboarding.application;

import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.onboarding.adapter.out.persistence.OnboardingStateRepository;
import com.helix.api.onboarding.domain.OnboardingStateEntity;
import com.helix.api.onboarding.domain.OnboardingStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class OnboardingService {

    private final OnboardingStateRepository repository;
    private final CurrentUserProvider currentUserProvider;

    public OnboardingService(OnboardingStateRepository repository, CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Returns the current user's onboarding status, bootstrapping a fresh {@code NOT_STARTED} row
     * for them on first access (ADR-021: one row per user, re-keyed from the old fixed-id singleton
     * by the V12 migration).
     */
    public OnboardingStateEntity get() {
        UUID ownerId = currentUserProvider.currentUserId();
        return repository.findByOwnerId(ownerId)
            .orElseGet(() -> repository.save(
                new OnboardingStateEntity(UUID.randomUUID(), OnboardingStatus.NOT_STARTED, OffsetDateTime.now(), ownerId)
            ));
    }

    /** Called when the first transformation is created. No-op if onboarding has already moved past this point. */
    public void advanceToFirstTransformationCreated() {
        advanceTo(OnboardingStatus.FIRST_TRANSFORMATION_CREATED);
    }

    /** Called when an experiment is created. No-op if onboarding is already complete. */
    public void advanceToComplete() {
        advanceTo(OnboardingStatus.COMPLETE);
    }

    /**
     * Unconditionally resets onboarding to NOT_STARTED. Used only by the whole-app data wipe
     * (ADR-019) — deleting everything should put a user back at the true first-use welcome screen.
     */
    public void reset() {
        var state = get();
        state.reset(OffsetDateTime.now());
        repository.save(state);
    }

    private void advanceTo(OnboardingStatus status) {
        var state = get();
        state.advanceTo(status, OffsetDateTime.now());
        repository.save(state);
    }
}
