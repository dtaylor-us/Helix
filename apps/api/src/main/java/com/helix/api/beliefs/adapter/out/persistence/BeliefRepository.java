package com.helix.api.beliefs.adapter.out.persistence;

import com.helix.api.beliefs.domain.BeliefEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BeliefRepository extends JpaRepository<BeliefEntity, UUID> {
    List<BeliefEntity> findAllByOrderByRevisedAtDesc();
    List<BeliefEntity> findTop20ByStatementContainingIgnoreCaseOrderByRevisedAtDesc(String query);
}