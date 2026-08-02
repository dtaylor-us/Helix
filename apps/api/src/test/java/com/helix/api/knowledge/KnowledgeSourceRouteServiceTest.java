package com.helix.api.knowledge;

import com.helix.api.evidence.adapter.out.persistence.EvidenceRepository;
import com.helix.api.evidence.domain.EvidenceDirection;
import com.helix.api.evidence.domain.EvidenceEntity;
import com.helix.api.evidence.domain.ProvenanceRecordType;
import com.helix.api.evidence.domain.ProvenanceSourceKind;
import com.helix.api.knowledge.application.KnowledgeSourceRouteService;
import com.helix.api.knowledge.domain.KnowledgeNodeType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KnowledgeSourceRouteServiceTest {

    @Test
    void evidenceRoutesToItsOwningBelief() {
        var evidenceRepository = Mockito.mock(EvidenceRepository.class);
        var service = new KnowledgeSourceRouteService(evidenceRepository);
        var evidenceId = UUID.randomUUID();
        var beliefId = UUID.randomUUID();
        Mockito.when(evidenceRepository.findById(evidenceId)).thenReturn(Optional.of(
            new EvidenceEntity(evidenceId, beliefId, null, null, "Stayed calm under pressure", null,
                EvidenceDirection.CHALLENGES, ProvenanceSourceKind.MANUAL_ENTRY, ProvenanceRecordType.MANUAL_ENTRY,
                null, null, OffsetDateTime.now())
        ));

        assertEquals("/knowledge?beliefId=" + beliefId,
            service.sourceRoute(KnowledgeNodeType.EVIDENCE, evidenceId));
    }

    @Test
    void missingEvidenceFallsBackToKnowledgePage() {
        var evidenceRepository = Mockito.mock(EvidenceRepository.class);
        var service = new KnowledgeSourceRouteService(evidenceRepository);
        var evidenceId = UUID.randomUUID();
        Mockito.when(evidenceRepository.findById(evidenceId)).thenReturn(Optional.empty());

        assertEquals("/knowledge", service.sourceRoute(KnowledgeNodeType.EVIDENCE, evidenceId));
    }
}
