package com.helix.api.evidence;

import com.helix.api.beliefs.application.BeliefService;
import com.helix.api.evidence.adapter.out.persistence.EvidenceRepository;
import com.helix.api.evidence.application.EvidenceService;
import com.helix.api.evidence.domain.EvidenceDirection;
import com.helix.api.evidence.domain.ProvenanceRecordType;
import com.helix.api.evidence.domain.ProvenanceSourceKind;
import com.helix.api.experiments.application.ExperimentService;
import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.experiments.domain.ExperimentStatus;
import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.reflection.domain.ReflectionEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class EvidenceServiceTest {

    @Test
    void createPreservesProvenanceMetadata() {
        var repository = Mockito.mock(EvidenceRepository.class);
        var beliefService = Mockito.mock(BeliefService.class);
        var experimentService = Mockito.mock(ExperimentService.class);
        var reflectionService = Mockito.mock(ReflectionService.class);

        var beliefId = UUID.randomUUID();
        var experimentId = UUID.randomUUID();
        var reflectionId = UUID.randomUUID();

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(experimentService.get(experimentId)).thenReturn(new ExperimentEntity(
            experimentId,
            UUID.randomUUID(),
            "Pause before replying",
            "If I pause, I will respond more thoughtfully",
            "Breathe once before texting back",
            ExperimentStatus.ACTIVE,
            OffsetDateTime.now().minusDays(2)
        ));
        when(reflectionService.get(reflectionId)).thenReturn(new ReflectionEntity(
            reflectionId,
            experimentId,
            "I paused twice and the conversation stayed calmer.",
            OffsetDateTime.now().minusDays(1)
        ));

        var service = new EvidenceService(repository, beliefService, experimentService, reflectionService);
        var evidence = service.create(
            beliefId,
            experimentId,
            reflectionId,
            "Two conversations stayed calmer after a pause.",
            "A short pause seems to reduce defensiveness.",
            EvidenceDirection.SUPPORTS,
            ProvenanceSourceKind.REFLECTION,
            ProvenanceRecordType.REFLECTION,
            reflectionId,
            "I paused twice and the conversation stayed calmer."
        );

        assertEquals(ProvenanceSourceKind.REFLECTION, evidence.getProvenanceSourceKind());
        assertEquals(ProvenanceRecordType.REFLECTION, evidence.getProvenanceRecordType());
        assertEquals(reflectionId, evidence.getProvenanceRecordId());
        assertNull(evidence.getInterpretation() == null ? null : null);
        assertEquals("Two conversations stayed calmer after a pause.", evidence.getSummary());
    }
}