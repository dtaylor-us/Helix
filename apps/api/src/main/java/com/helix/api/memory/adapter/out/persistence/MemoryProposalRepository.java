package com.helix.api.memory.adapter.out.persistence;

import com.helix.api.memory.domain.MemoryProposalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemoryProposalRepository extends JpaRepository<MemoryProposalEntity, UUID> {

    List<MemoryProposalEntity> findAllByOrderByRevisedAtDesc();
    // ADR-021: used by DataExportService/DataDeletionService only -- see WeeklyRetrospectiveRepository.
    List<MemoryProposalEntity> findAllByOwnerId(UUID ownerId);
    void deleteAllByOwnerId(UUID ownerId);
}