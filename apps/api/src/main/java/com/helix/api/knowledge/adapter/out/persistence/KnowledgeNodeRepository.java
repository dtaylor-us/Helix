package com.helix.api.knowledge.adapter.out.persistence;

import com.helix.api.knowledge.domain.KnowledgeNodeEntity;
import com.helix.api.knowledge.domain.KnowledgeNodeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeNodeRepository extends JpaRepository<KnowledgeNodeEntity, UUID> {
    Optional<KnowledgeNodeEntity> findByOwnerIdAndNodeTypeAndSourceRecordId(UUID ownerId, KnowledgeNodeType nodeType, UUID sourceRecordId);
    List<KnowledgeNodeEntity> findByOwnerIdAndNodeType(UUID ownerId, KnowledgeNodeType nodeType);
    List<KnowledgeNodeEntity> findByOwnerIdAndIdIn(UUID ownerId, java.util.Collection<UUID> ids);
    void deleteAllByOwnerId(UUID ownerId);
}
