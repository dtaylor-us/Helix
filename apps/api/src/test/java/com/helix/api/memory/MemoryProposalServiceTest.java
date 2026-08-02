package com.helix.api.memory;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.evidence.application.EvidenceService;
import com.helix.api.beliefs.application.BeliefService;
import com.helix.api.experiments.application.ExperimentService;
import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.experiments.domain.ExperimentStatus;
import com.helix.api.identity.application.CurrentUserProvider;
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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        var aiAssistantPort = Mockito.mock(AiAssistantPort.class);
        var currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        var ownerId = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(ownerId);
        var service = new MemoryProposalService(
            repository,
            revisionRepository,
            experimentService,
            beliefService,
            reflectionService,
            evidenceService,
            wisdomService,
            retrospectiveService,
            aiAssistantPort,
            currentUserProvider
        );

        var proposal = service.create(
            "Smaller actions protect consistency.",
            MemorySourceKind.AI_DERIVED,
            MemorySourceRecordType.REFLECTION,
            sourceRecordId,
            "This line came from the reflection note."
        );
        when(repository.findByIdAndOwnerId(proposal.getId(), ownerId)).thenReturn(Optional.of(proposal));

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

    @Test
    void createRejectsUnknownSourceRecordsWithActionableMessage() {
        var repository = Mockito.mock(MemoryProposalRepository.class);
        var revisionRepository = Mockito.mock(MemoryProposalRevisionRepository.class);
        var experimentService = Mockito.mock(ExperimentService.class);
        var beliefService = Mockito.mock(BeliefService.class);
        var reflectionService = Mockito.mock(ReflectionService.class);
        var evidenceService = Mockito.mock(EvidenceService.class);
        var wisdomService = Mockito.mock(WisdomService.class);
        var retrospectiveService = Mockito.mock(WeeklyRetrospectiveService.class);
        var missingReflectionId = UUID.randomUUID();

        when(reflectionService.get(missingReflectionId)).thenThrow(new NoSuchElementException("Reflection not found"));

        var aiAssistantPort = Mockito.mock(AiAssistantPort.class);
        var currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.currentUserId()).thenReturn(UUID.randomUUID());
        var service = new MemoryProposalService(
            repository,
            revisionRepository,
            experimentService,
            beliefService,
            reflectionService,
            evidenceService,
            wisdomService,
            retrospectiveService,
            aiAssistantPort,
            currentUserProvider
        );

        var error = assertThrows(IllegalArgumentException.class, () -> service.create(
            "Smaller actions protect consistency.",
            MemorySourceKind.AI_DERIVED,
            MemorySourceRecordType.REFLECTION,
            missingReflectionId,
            "This line came from the reflection note."
        ));

        assertEquals("That source record couldn't be found — check the ID/type and try again.", error.getMessage());
    }

    @Test
    void proposeFromReflectionReturnsAiDraftWithoutPersistingAnything() {
        var repository = Mockito.mock(MemoryProposalRepository.class);
        var revisionRepository = Mockito.mock(MemoryProposalRevisionRepository.class);
        var experimentService = Mockito.mock(ExperimentService.class);
        var beliefService = Mockito.mock(BeliefService.class);
        var reflectionService = Mockito.mock(ReflectionService.class);
        var evidenceService = Mockito.mock(EvidenceService.class);
        var wisdomService = Mockito.mock(WisdomService.class);
        var retrospectiveService = Mockito.mock(WeeklyRetrospectiveService.class);
        var aiAssistantPort = Mockito.mock(AiAssistantPort.class);

        var experimentId = UUID.randomUUID();
        var reflectionId = UUID.randomUUID();
        when(reflectionService.get(reflectionId)).thenReturn(new ReflectionEntity(
            reflectionId, experimentId, "I paused before responding and felt steadier.",
            OffsetDateTime.now().minusHours(2)
        ));
        when(experimentService.get(experimentId)).thenReturn(new ExperimentEntity(
            experimentId, UUID.randomUUID(), "Pause before responding", "Pausing helps",
            "Breathe once", ExperimentStatus.ACTIVE, OffsetDateTime.now().minusDays(1)
        ));
        when(aiAssistantPort.proposeMemory(any())).thenReturn(new AiAssistantPort.AiMemoryProposal(
            "I tend to feel steadier when I pause before reacting.", "openai", "gpt-4o-mini", false
        ));

        var currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.currentUserId()).thenReturn(UUID.randomUUID());
        var service = new MemoryProposalService(
            repository, revisionRepository, experimentService, beliefService,
            reflectionService, evidenceService, wisdomService, retrospectiveService, aiAssistantPort,
            currentUserProvider
        );

        var draft = service.proposeFromReflection(reflectionId);

        assertEquals("I tend to feel steadier when I pause before reacting.", draft.statement());
        assertEquals("AI", draft.source());
        Mockito.verifyNoInteractions(repository);
        Mockito.verifyNoInteractions(revisionRepository);
    }

    @Test
    void proposeFromReflectionReturnsNullStatementWhenNothingWorthProposing() {
        var repository = Mockito.mock(MemoryProposalRepository.class);
        var revisionRepository = Mockito.mock(MemoryProposalRevisionRepository.class);
        var experimentService = Mockito.mock(ExperimentService.class);
        var beliefService = Mockito.mock(BeliefService.class);
        var reflectionService = Mockito.mock(ReflectionService.class);
        var evidenceService = Mockito.mock(EvidenceService.class);
        var wisdomService = Mockito.mock(WisdomService.class);
        var retrospectiveService = Mockito.mock(WeeklyRetrospectiveService.class);
        var aiAssistantPort = Mockito.mock(AiAssistantPort.class);

        var experimentId = UUID.randomUUID();
        var reflectionId = UUID.randomUUID();
        when(reflectionService.get(reflectionId)).thenReturn(new ReflectionEntity(
            reflectionId, experimentId, "Nothing notable happened today.", OffsetDateTime.now()
        ));
        when(experimentService.get(experimentId)).thenReturn(new ExperimentEntity(
            experimentId, UUID.randomUUID(), "Pause before responding", "Pausing helps",
            "Breathe once", ExperimentStatus.ACTIVE, OffsetDateTime.now().minusDays(1)
        ));
        when(aiAssistantPort.proposeMemory(any())).thenReturn(new AiAssistantPort.AiMemoryProposal(
            null, "openai", "gpt-4o-mini", false
        ));

        var currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.currentUserId()).thenReturn(UUID.randomUUID());
        var service = new MemoryProposalService(
            repository, revisionRepository, experimentService, beliefService,
            reflectionService, evidenceService, wisdomService, retrospectiveService, aiAssistantPort,
            currentUserProvider
        );

        var draft = service.proposeFromReflection(reflectionId);

        assertNull(draft.statement());
        assertEquals("AI", draft.source());
    }
}