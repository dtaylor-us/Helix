package com.helix.api.transformation.adapter.out.persistence;

import com.helix.api.transformation.domain.TransformationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransformationRepository extends JpaRepository<TransformationEntity, UUID> {
    List<TransformationEntity> findAllByOwnerId(UUID ownerId);
    Optional<TransformationEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    void deleteAllByOwnerId(UUID ownerId);
}
