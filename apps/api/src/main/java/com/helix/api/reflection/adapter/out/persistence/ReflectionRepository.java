package com.helix.api.reflection.adapter.out.persistence;

import com.helix.api.reflection.domain.ReflectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReflectionRepository extends JpaRepository<ReflectionEntity, UUID> {
    List<ReflectionEntity> findByExperimentIdOrderByCreatedAtDesc(UUID experimentId);
    List<ReflectionEntity> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    List<ReflectionEntity> findByOwnerIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(UUID ownerId, OffsetDateTime threshold);
    List<ReflectionEntity> findTop20ByOwnerIdAndContentContainingIgnoreCaseOrderByCreatedAtDesc(UUID ownerId, String query);
    Optional<ReflectionEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    void deleteAllByOwnerId(UUID ownerId);
}
