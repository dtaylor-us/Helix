package com.helix.api.knowledge.adapter.out.persistence;

import com.helix.api.knowledge.domain.KnowledgeProjectionCheckpointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KnowledgeProjectionCheckpointRepository extends JpaRepository<KnowledgeProjectionCheckpointEntity, UUID> {
    Optional<KnowledgeProjectionCheckpointEntity> findBySourceModule(String sourceModule);
}
