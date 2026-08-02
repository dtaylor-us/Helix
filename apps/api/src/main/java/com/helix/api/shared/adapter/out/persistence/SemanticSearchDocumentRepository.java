package com.helix.api.shared.adapter.out.persistence;

import com.helix.api.shared.domain.SemanticSearchDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SemanticSearchDocumentRepository extends JpaRepository<SemanticSearchDocumentEntity, UUID> {
    List<SemanticSearchDocumentEntity> findAllByOrderByIndexedAtDesc();
    // ADR-021: used by DataDeletionService only -- see SemanticIndexingService's gap-list javadoc for
    // why deleteAllInBatch() itself (used by rebuild()) is still NOT owner-scoped.
    void deleteAllByOwnerId(UUID ownerId);
}