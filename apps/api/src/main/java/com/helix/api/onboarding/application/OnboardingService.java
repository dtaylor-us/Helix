package com.helix.api.onboarding.application;

import com.helix.api.onboarding.adapter.out.persistence.OnboardingStateRepository;
import com.helix.api.onboarding.domain.OnboardingStateEntity;
import com.helix.api.onboarding.domain.OnboardingStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class OnboardingService {

    private final OnboardingStateRepository repository;

    public OnboardingService(OnboardingStateRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns the current onboarding status. The singleton row is seeded by migration V10, but
     * this defensively bootstraps it as {@code NOT_STARTED} if it's ever missing (e.g. a database
     * reset that skipped the seed insert) rather than throwing.
     */
    public OnboardingStateEntity get() {
        return repository.findById(OnboardingStateEntity.SINGLETON_ID)
            .orElseGet(() -> repository.save(
                new OnboardingStateEntity(OnboardingStateEntity.SINGLETON_ID, OnboardingStatus.NOT_STARTED, OffsetDateTime.now())
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
