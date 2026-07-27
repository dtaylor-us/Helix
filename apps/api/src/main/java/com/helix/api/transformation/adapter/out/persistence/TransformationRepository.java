package com.helix.api.transformation.adapter.out.persistence;

import com.helix.api.transformation.domain.TransformationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransformationRepository extends JpaRepository<TransformationEntity, UUID> {
}
