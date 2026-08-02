package com.helix.api.beliefs.adapter.out.persistence;

import com.helix.api.beliefs.domain.BeliefEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BeliefRepository extends JpaRepository<BeliefEntity, UUID> {
    List<BeliefEntity> findAllByOwnerIdOrderByRevisedAtDesc(UUID ownerId);
    List<BeliefEntity> findTop20ByOwnerIdAndStatementContainingIgnoreCaseOrderByRevisedAtDesc(UUID ownerId, String query);
    Optional<BeliefEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    void deleteAllByOwnerId(UUID ownerId);
}