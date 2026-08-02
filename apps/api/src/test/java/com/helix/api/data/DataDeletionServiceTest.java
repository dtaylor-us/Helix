package com.helix.api.data;

import com.helix.api.beliefs.adapter.out.persistence.BeliefRepository;
import com.helix.api.beliefs.adapter.out.persistence.BeliefRevisionRepository;
import com.helix.api.data.application.DataDeletionService;
import com.helix.api.evidence.adapter.out.persistence.EvidenceRepository;
import com.helix.api.experiments.adapter.out.persistence.ExperimentRepository;
import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.memory.adapter.out.persistence.MemoryProposalRepository;
import com.helix.api.memory.adapter.out.persistence.MemoryProposalRevisionRepository;
import com.helix.api.onboarding.application.OnboardingService;
import com.helix.api.reflection.adapter.out.persistence.ReflectionRepository;
import com.helix.api.shared.adapter.out.persistence.SemanticSearchDocumentRepository;
import com.helix.api.suggestions.adapter.out.persistence.SuggestionRepository;
import com.helix.api.transformation.adapter.out.persistence.TransformationRepository;
import com.helix.api.wisdom.adapter.out.persistence.WeeklyRetrospectiveRepository;
import com.helix.api.wisdom.adapter.out.persistence.WisdomEntryRepository;
import com.helix.api.wisdom.adapter.out.persistence.WisdomRevisionRepository;
import com.helix.api.wisdom.adapter.out.persistence.WisdomSourceLinkRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.mockito.Mockito.when;

class DataDeletionServiceTest {

    @Test
    void deleteEverythingClearsOnlyTheCallersOwnRecordsAndResetsOnboarding() {
        var transformationRepository = Mockito.mock(TransformationRepository.class);
        var experimentRepository = Mockito.mock(ExperimentRepository.class);
        var reflectionRepository = Mockito.mock(ReflectionRepository.class);
        var suggestionRepository = Mockito.mock(SuggestionRepository.class);
        var beliefRepository = Mockito.mock(BeliefRepository.class);
        var beliefRevisionRepository = Mockito.mock(BeliefRevisionRepository.class);
        var evidenceRepository = Mockito.mock(EvidenceRepository.class);
        var weeklyRetrospectiveRepository = Mockito.mock(WeeklyRetrospectiveRepository.class);
        var wisdomEntryRepository = Mockito.mock(WisdomEntryRepository.class);
        var wisdomRevisionRepository = Mockito.mock(WisdomRevisionRepository.class);
        var wisdomSourceLinkRepository = Mockito.mock(WisdomSourceLinkRepository.class);
        var memoryProposalRepository = Mockito.mock(MemoryProposalRepository.class);
        var memoryProposalRevisionRepository = Mockito.mock(MemoryProposalRevisionRepository.class);
        var semanticSearchDocumentRepository = Mockito.mock(SemanticSearchDocumentRepository.class);
        var onboardingService = Mockito.mock(OnboardingService.class);
        var currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        var ownerId = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(ownerId);

        var service = new DataDeletionService(
            transformationRepository, experimentRepository, reflectionRepository, suggestionRepository,
            beliefRepository, beliefRevisionRepository, evidenceRepository, weeklyRetrospectiveRepository,
            wisdomEntryRepository, wisdomRevisionRepository, wisdomSourceLinkRepository,
            memoryProposalRepository, memoryProposalRevisionRepository, semanticSearchDocumentRepository,
            onboardingService, currentUserProvider
        );

        service.deleteEverything();

        Mockito.verify(transformationRepository).deleteAllByOwnerId(ownerId);
        Mockito.verify(experimentRepository).deleteAllByOwnerId(ownerId);
        Mockito.verify(reflectionRepository).deleteAllByOwnerId(ownerId);
        Mockito.verify(suggestionRepository).deleteAllByOwnerId(ownerId);
        Mockito.verify(beliefRepository).deleteAllByOwnerId(ownerId);
        Mockito.verify(beliefRevisionRepository).deleteAllByOwnerId(ownerId);
        Mockito.verify(evidenceRepository).deleteAllByOwnerId(ownerId);
        Mockito.verify(weeklyRetrospectiveRepository).deleteAllByOwnerId(ownerId);
        Mockito.verify(wisdomEntryRepository).deleteAllByOwnerId(ownerId);
        Mockito.verify(wisdomRevisionRepository).deleteAllByOwnerId(ownerId);
        Mockito.verify(wisdomSourceLinkRepository).deleteAllByOwnerId(ownerId);
        Mockito.verify(memoryProposalRepository).deleteAllByOwnerId(ownerId);
        Mockito.verify(memoryProposalRevisionRepository).deleteAllByOwnerId(ownerId);
        Mockito.verify(semanticSearchDocumentRepository).deleteAllByOwnerId(ownerId);
        Mockito.verify(onboardingService).reset();
    }
}
