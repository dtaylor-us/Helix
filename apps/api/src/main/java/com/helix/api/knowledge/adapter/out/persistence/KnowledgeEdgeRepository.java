package com.helix.api.knowledge.adapter.out.persistence;

import com.helix.api.knowledge.domain.KnowledgeEdgeEntity;
import com.helix.api.knowledge.domain.KnowledgeEdgeStatus;
import com.helix.api.knowledge.domain.KnowledgeEdgeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KnowledgeEdgeRepository extends JpaRepository<KnowledgeEdgeEntity, UUID> {
    List<KnowledgeEdgeEntity> findBySourceNodeIdOrTargetNodeId(UUID sourceNodeId, UUID targetNodeId);
    List<KnowledgeEdgeEntity> findByStatus(KnowledgeEdgeStatus status);
    List<KnowledgeEdgeEntity> findByRelationshipType(KnowledgeEdgeType relationshipType);
}
