package com.helix.api.experiments.adapter.out.persistence;

import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.experiments.domain.ExperimentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperimentRepository extends JpaRepository<ExperimentEntity, UUID> {
    Optional<ExperimentEntity> findFirstByStatusOrderByCreatedAtDesc(ExperimentStatus status);
    List<ExperimentEntity> findByTransformationId(UUID transformationId);
}
