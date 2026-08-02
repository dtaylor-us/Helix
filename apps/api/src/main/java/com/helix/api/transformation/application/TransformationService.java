package com.helix.api.transformation.application;

import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.onboarding.application.OnboardingService;
import com.helix.api.transformation.adapter.out.persistence.TransformationRepository;
import com.helix.api.transformation.domain.TransformationEntity;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Transformation is the root aggregate of the whole domain (ADR-021) — every other entity in this
 * app traces back to one, directly or transitively. {@link #get(UUID)} is this service's single
 * enforcement chokepoint: it 404s (not 403, to avoid confirming a record exists to someone who
 * doesn't own it) on any id that either doesn't exist or belongs to a different owner. Every other
 * service that resolves a transformationId should call through here rather than re-implementing the
 * ownership check.
 */
@Service
public class TransformationService {

    private final TransformationRepository repository;
    private final OnboardingService onboardingService;
    private final CurrentUserProvider currentUserProvider;

    public TransformationService(
        TransformationRepository repository, OnboardingService onboardingService, CurrentUserProvider currentUserProvider
    ) {
        this.repository = repository;
        this.onboardingService = onboardingService;
        this.currentUserProvider = currentUserProvider;
    }

    public TransformationEntity create(String title, String purpose) {
        return create(title, purpose, null, null);
    }

    public TransformationEntity create(String title, String purpose, String desiredIdentity, String obstacle) {
        var entity = new TransformationEntity(
            UUID.randomUUID(), title.trim(), purpose, desiredIdentity, obstacle, OffsetDateTime.now(),
            currentUserProvider.currentUserId()
        );
        var saved = repository.save(entity);
        // Phase 7: server-persisted onboarding state. No-op once onboarding has already moved
        // past this point, so this is safe to call on every creation, not just the very first.
        onboardingService.advanceToFirstTransformationCreated();
        return saved;
    }

    public List<TransformationEntity> list() {
        return repository.findAllByOwnerId(currentUserProvider.currentUserId());
    }

    public TransformationEntity get(UUID id) {
        return repository.findByIdAndOwnerId(id, currentUserProvider.currentUserId())
            .orElseThrow(() -> new NoSuchElementException("Transformation not found"));
    }
}
