package com.helix.api.memory.adapter.out.persistence;

import com.helix.api.memory.domain.MemoryProposalRevisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemoryProposalRevisionRepository extends JpaRepository<MemoryProposalRevisionEntity, UUID> {

    List<MemoryProposalRevisionEntity> findByMemoryProposalIdOrderByCreatedAtDesc(UUID memoryProposalId);
    // ADR-021: used by DataExportService/DataDeletionService only -- see WeeklyRetrospectiveRepository.
    List<MemoryProposalRevisionEntity> findAllByOwnerId(UUID ownerId);
    void deleteAllByOwnerId(UUID ownerId);
}