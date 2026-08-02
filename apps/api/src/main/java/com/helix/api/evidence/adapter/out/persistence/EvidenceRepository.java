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
// ADR-021: EvidenceEntity has no single owning parent (it can reference a belief, an experiment,
// and a reflection at once), so there's no findByIdAndOwnerId here -- EvidenceService.get() instead
// does an explicit ownerId equality check against the loaded row before returning it, since it's
// called directly (not always via BeliefService) by EvidenceController.