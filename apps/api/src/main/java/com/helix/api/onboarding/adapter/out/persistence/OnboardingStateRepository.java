package com.helix.api.onboarding.adapter.out.persistence;

import com.helix.api.onboarding.domain.OnboardingStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OnboardingStateRepository extends JpaRepository<OnboardingStateEntity, UUID> {
    Optional<OnboardingStateEntity> findByOwnerId(UUID ownerId);
}
