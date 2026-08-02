package com.helix.api.knowledge.adapter.out.persistence;

import com.helix.api.knowledge.domain.KnowledgeEdgeEntity;
import com.helix.api.knowledge.domain.KnowledgeEdgeStatus;
import com.helix.api.knowledge.domain.KnowledgeEdgeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KnowledgeEdgeRepository extends JpaRepository<KnowledgeEdgeEntity, UUID> {
    List<KnowledgeEdgeEntity> findByOwnerIdAndStatus(UUID ownerId, KnowledgeEdgeStatus status);
    List<KnowledgeEdgeEntity> findByOwnerIdAndRelationshipType(UUID ownerId, KnowledgeEdgeType relationshipType);
    java.util.Optional<KnowledgeEdgeEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    void deleteAllByOwnerId(UUID ownerId);
}
