package com.helix.api.reflection.adapter.out.persistence;

import com.helix.api.reflection.domain.ReflectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ReflectionRepository extends JpaRepository<ReflectionEntity, UUID> {
    List<ReflectionEntity> findAllByOrderByCreatedAtDesc();
    List<ReflectionEntity> findByExperimentIdOrderByCreatedAtDesc(UUID experimentId);
    List<ReflectionEntity> findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(OffsetDateTime threshold);
    List<ReflectionEntity> findTop20ByContentContainingIgnoreCaseOrderByCreatedAtDesc(String query);
}
