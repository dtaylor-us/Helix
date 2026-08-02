package com.helix.api.knowledge.adapter.out.persistence;

import com.helix.api.knowledge.domain.KnowledgeNodeEntity;
import com.helix.api.knowledge.domain.KnowledgeNodeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KnowledgeNodeRepository extends JpaRepository<KnowledgeNodeEntity, UUID> {
    Optional<KnowledgeNodeEntity> findByNodeTypeAndSourceRecordId(KnowledgeNodeType nodeType, UUID sourceRecordId);
}
