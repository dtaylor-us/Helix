package com.helix.api.evidence.adapter.out.persistence;

import com.helix.api.evidence.domain.EvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EvidenceRepository extends JpaRepository<EvidenceEntity, UUID> {
    List<EvidenceEntity> findByBeliefIdOrderByCreatedAtDesc(UUID beliefId);
    List<EvidenceEntity> findAllByOwnerId(UUID ownerId);
    void deleteAllByOwnerId(UUID ownerId);
    List<EvidenceEntity> findTop20ByOwnerIdAndSummaryContainingIgnoreCaseOrOwnerIdAndInterpretationContainingIgnoreCaseOrderByCreatedAtDesc(
        UUID ownerIdForSummary, String summaryQuery, UUID ownerIdForInterpretation, String interpretationQuery
    );
}
// ADR-021: get(id) is not independently owner-scoped below -- EvidenceEntity has no single owning
// parent (it can reference a belief, an experiment, and a reflection at once); every existing call
// site resolves the belief via BeliefService.get() first, which already enforces ownership. This is
// flagged in the ADR-021 gap list as needing a direct owner_id check on EvidenceService.get() too,
// since that method is called directly (not always via BeliefService) by EvidenceController.