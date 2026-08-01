package com.helix.api.transformation.application;

import com.helix.api.onboarding.application.OnboardingService;
import com.helix.api.transformation.adapter.out.persistence.TransformationRepository;
import com.helix.api.transformation.domain.TransformationEntity;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class TransformationService {

    private final TransformationRepository repository;
    private final OnboardingService onboardingService;

    public TransformationService(TransformationRepository repository, OnboardingService onboardingService) {
        this.repository = repository;
        this.onboardingService = onboardingService;
    }

    public TransformationEntity create(String title, String purpose) {
        return create(title, purpose, null, null);
    }

    public TransformationEntity create(String title, String purpose, String desiredIdentity, String obstacle) {
        var entity = new TransformationEntity(
            UUID.randomUUID(), title.trim(), purpose, desiredIdentity, obstacle, OffsetDateTime.now()
        );
        var saved = repository.save(entity);
        // Phase 7: server-persisted onboarding state. No-op once onboarding has already moved
        // past this point, so this is safe to call on every creation, not just the very first.
        onboardingService.advanceToFirstTransformationCreated();
        return saved;
    }

    public List<TransformationEntity> list() {
        return repository.findAll();
    }

    public TransformationEntity get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Transformation not found"));
    }
}
