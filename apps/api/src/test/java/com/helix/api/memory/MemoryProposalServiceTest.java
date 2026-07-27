package com.helix.api.memory;

import com.helix.api.evidence.application.EvidenceService;
import com.helix.api.beliefs.application.BeliefService;
import com.helix.api.experiments.application.ExperimentService;
import com.helix.api.memory.adapter.out.persistence.MemoryProposalRepository;
import com.helix.api.memory.adapter.out.persistence.MemoryProposalRevisionRepository;
import com.helix.api.memory.application.MemoryProposalService;
import com.helix.api.memory.domain.MemoryProposalEntity;
import com.helix.api.memory.domain.MemoryProposalStatus;
import com.helix.api.memory.domain.MemorySourceKind;
import com.helix.api.memory.domain.MemorySourceRecordType;
import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.wisdom.application.WeeklyRetrospectiveService;
import com.helix.api.wisdom.application.WisdomService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class MemoryProposalServiceTest {

    @Test
    void reviseAndReviewMemoryProposalTracksLifecycleTransitions() {
        var repository = Mockito.mock(MemoryProposalRepository.class);
        var revisionRepository = Mockito.mock(MemoryProposalRevisionRepository.class);
        var experimentService = Mockito.mock(ExperimentService.class);
        var beliefService = Mockito.mock(BeliefService.class);
        var reflectionService = Mockito.mock(ReflectionService.class);
        var evidenceService = Mockito.mock(EvidenceService.class);
        var wisdomService = Mockito.mock(WisdomService.class);
        var retrospectiveService = Mockito.mock(WeeklyRetrospectiveService.class);

        var sourceRecordId = UUID.randomUUID();
        when(reflectionService.get(sourceRecordId)).thenReturn(new ReflectionEntity(
            sourceRecordId,
            UUID.randomUUID(),
            "A quieter cadence made the experiment easier.",
            OffsetDateTime.now().minusDays(1)
        ));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(revisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new MemoryProposalService(
            repository,
            revisionRepository,
            experimentService,
            beliefService,
            reflectionService,
            evidenceService,
            wisdomService,
            retrospectiveService
        );

        var proposal = service.create(
            "Smaller actions protect consistency.",
            MemorySourceKind.AI_DERIVED,
            MemorySourceRecordType.REFLECTION,
            sourceRecordId,
            "This line came from the reflection note."
        );
        when(repository.findById(proposal.getId())).thenReturn(Optional.of(proposal));

        var revision = service.revise(
            proposal.getId(),
            "Smaller actions often protect consistency.",
            "Clarified the language before review.",
            "Updated after reading the reflection again."
        );
        assertEquals(MemoryProposalStatus.PROPOSED, proposal.getStatus());

        var approval = service.accept(proposal.getId(), "This memory is ready to keep.");

        assertEquals("Smaller actions protect consistency.", revision.getPreviousStatement());
        assertEquals("Smaller actions often protect consistency.", revision.getNewStatement());
        assertEquals(MemoryProposalStatus.CONFIRMED, proposal.getStatus());
        assertNotNull(approval.getCreatedAt());
    }
}