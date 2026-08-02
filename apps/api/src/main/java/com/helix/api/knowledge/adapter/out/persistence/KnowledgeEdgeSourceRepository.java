package com.helix.api.knowledge.adapter.out.persistence;

import com.helix.api.knowledge.domain.KnowledgeEdgeSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KnowledgeEdgeSourceRepository extends JpaRepository<KnowledgeEdgeSourceEntity, UUID> {
    List<KnowledgeEdgeSourceEntity> findByOwnerIdAndKnowledgeEdgeIdIn(UUID ownerId, List<UUID> knowledgeEdgeIds);
    void deleteAllByOwnerId(UUID ownerId);
}
