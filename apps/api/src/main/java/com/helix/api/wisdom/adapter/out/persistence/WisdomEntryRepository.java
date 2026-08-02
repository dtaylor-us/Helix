package com.helix.api.wisdom.adapter.out.persistence;

import com.helix.api.wisdom.domain.WisdomEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WisdomEntryRepository extends JpaRepository<WisdomEntryEntity, UUID> {
    List<WisdomEntryEntity> findAllByOwnerIdOrderByRevisedAtDesc(UUID ownerId);
    List<WisdomEntryEntity> findTop20ByOwnerIdAndStatementContainingIgnoreCaseOrderByRevisedAtDesc(UUID ownerId, String query);
    java.util.Optional<WisdomEntryEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    // ADR-021: used by DataExportService/DataDeletionService only -- unscoped bulk export/delete.
    List<WisdomEntryEntity> findAllByOwnerId(UUID ownerId);
    void deleteAllByOwnerId(UUID ownerId);
}
