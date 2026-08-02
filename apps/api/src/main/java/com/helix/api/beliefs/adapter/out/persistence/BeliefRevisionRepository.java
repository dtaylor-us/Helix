package com.helix.api.beliefs.adapter.out.persistence;

import com.helix.api.beliefs.domain.BeliefRevisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BeliefRevisionRepository extends JpaRepository<BeliefRevisionEntity, UUID> {
    List<BeliefRevisionEntity> findByBeliefIdOrderByCreatedAtDesc(UUID beliefId);
    // ADR-021: used by DataExportService/DataDeletionService only -- every other read path
    // (BeliefService.revisionHistory) resolves the parent belief via BeliefService.get() first,
    // which already enforces ownership.
    List<BeliefRevisionEntity> findAllByOwnerId(UUID ownerId);
    void deleteAllByOwnerId(UUID ownerId);
}