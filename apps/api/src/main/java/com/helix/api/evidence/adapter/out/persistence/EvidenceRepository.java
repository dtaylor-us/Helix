package com.helix.api.evidence.adapter.out.persistence;

import com.helix.api.evidence.domain.EvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvidenceRepository extends JpaRepository<EvidenceEntity, UUID> {
    List<EvidenceEntity> findByBeliefIdOrderByCreatedAtDesc(UUID beliefId);
    List<EvidenceEntity> findTop20BySummaryContainingIgnoreCaseOrInterpretationContainingIgnoreCaseOrderByCreatedAtDesc(
        String summaryQuery,
        String interpretationQuery
    );
}