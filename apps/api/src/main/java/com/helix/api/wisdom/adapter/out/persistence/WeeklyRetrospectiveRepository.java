package com.helix.api.wisdom.adapter.out.persistence;

import com.helix.api.wisdom.domain.WeeklyRetrospectiveEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WeeklyRetrospectiveRepository extends JpaRepository<WeeklyRetrospectiveEntity, UUID> {
    List<WeeklyRetrospectiveEntity> findTop20ByOwnerIdAndSummaryContainingIgnoreCaseOrderByCreatedAtDesc(UUID ownerId, String query);
    List<WeeklyRetrospectiveEntity> findTop10ByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    java.util.Optional<WeeklyRetrospectiveEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    // ADR-021: used by DataExportService/DataDeletionService only -- unscoped bulk export/delete.
    List<WeeklyRetrospectiveEntity> findAllByOwnerId(UUID ownerId);
    void deleteAllByOwnerId(UUID ownerId);
}
