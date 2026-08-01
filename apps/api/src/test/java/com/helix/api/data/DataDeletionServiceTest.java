package com.helix.api.data;

import com.helix.api.beliefs.adapter.out.persistence.BeliefRepository;
import com.helix.api.beliefs.adapter.out.persistence.BeliefRevisionRepository;
import com.helix.api.data.application.DataDeletionService;
import com.helix.api.evidence.adapter.out.persistence.EvidenceRepository;
import com.helix.api.experiments.adapter.out.persistence.ExperimentRepository;
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

class DataDeletionServiceTest {

    @Test
    void deleteEverythingClearsEveryRepositoryAndResetsOnboarding() {
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

        var service = new DataDeletionService(
            transformationRepository, experimentRepository, reflectionRepository, suggestionRepository,
            beliefRepository, beliefRevisionRepository, evidenceRepository, weeklyRetrospectiveRepository,
            wisdomEntryRepository, wisdomRevisionRepository, wisdomSourceLinkRepository,
            memoryProposalRepository, memoryProposalRevisionRepository, semanticSearchDocumentRepository,
            onboardingService
        );

        service.deleteEverything();

        Mockito.verify(transformationRepository).deleteAllInBatch();
        Mockito.verify(experimentRepository).deleteAllInBatch();
        Mockito.verify(reflectionRepository).deleteAllInBatch();
        Mockito.verify(suggestionRepository).deleteAllInBatch();
        Mockito.verify(beliefRepository).deleteAllInBatch();
        Mockito.verify(beliefRevisionRepository).deleteAllInBatch();
        Mockito.verify(evidenceRepository).deleteAllInBatch();
        Mockito.verify(weeklyRetrospectiveRepository).deleteAllInBatch();
        Mockito.verify(wisdomEntryRepository).deleteAllInBatch();
        Mockito.verify(wisdomRevisionRepository).deleteAllInBatch();
        Mockito.verify(wisdomSourceLinkRepository).deleteAllInBatch();
        Mockito.verify(memoryProposalRepository).deleteAllInBatch();
        Mockito.verify(memoryProposalRevisionRepository).deleteAllInBatch();
        Mockito.verify(semanticSearchDocumentRepository).deleteAllInBatch();
        Mockito.verify(onboardingService).reset();
    }
}
