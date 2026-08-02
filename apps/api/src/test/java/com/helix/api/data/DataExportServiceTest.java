package com.helix.api.data;

import com.helix.api.beliefs.adapter.out.persistence.BeliefRepository;
import com.helix.api.beliefs.adapter.out.persistence.BeliefRevisionRepository;
import com.helix.api.data.application.DataExportService;
import com.helix.api.evidence.adapter.out.persistence.EvidenceRepository;
import com.helix.api.experiments.adapter.out.persistence.ExperimentRepository;
import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.memory.adapter.out.persistence.MemoryProposalRepository;
import com.helix.api.memory.adapter.out.persistence.MemoryProposalRevisionRepository;
import com.helix.api.onboarding.application.OnboardingService;
import com.helix.api.onboarding.domain.OnboardingStateEntity;
import com.helix.api.onboarding.domain.OnboardingStatus;
import com.helix.api.reflection.adapter.out.persistence.ReflectionRepository;
import com.helix.api.suggestions.adapter.out.persistence.SuggestionRepository;
import com.helix.api.transformation.adapter.out.persistence.TransformationRepository;
import com.helix.api.transformation.domain.TransformationEntity;
import com.helix.api.wisdom.adapter.out.persistence.WeeklyRetrospectiveRepository;
import com.helix.api.wisdom.adapter.out.persistence.WisdomEntryRepository;
import com.helix.api.wisdom.adapter.out.persistence.WisdomRevisionRepository;
import com.helix.api.wisdom.adapter.out.persistence.WisdomSourceLinkRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class DataExportServiceTest {

    @Test
    void exportIncludesEveryModuleAndCurrentOnboardingStatus() {
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
        var onboardingService = Mockito.mock(OnboardingService.class);
        var currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        var ownerId = UUID.randomUUID();
        when(currentUserProvider.currentUserId()).thenReturn(ownerId);

        var transformationId = UUID.randomUUID();
        when(transformationRepository.findAllByOwnerId(ownerId)).thenReturn(List.of(
            new TransformationEntity(transformationId, "Become more peaceful", "Practice steadiness", OffsetDateTime.now())
        ));
        when(onboardingService.get()).thenReturn(
            new OnboardingStateEntity(OnboardingStateEntity.SINGLETON_ID, OnboardingStatus.COMPLETE, OffsetDateTime.now())
        );

        var service = new DataExportService(
            transformationRepository, experimentRepository, reflectionRepository, suggestionRepository,
            beliefRepository, beliefRevisionRepository, evidenceRepository, weeklyRetrospectiveRepository,
            wisdomEntryRepository, wisdomRevisionRepository, wisdomSourceLinkRepository,
            memoryProposalRepository, memoryProposalRevisionRepository, onboardingService, currentUserProvider
        );

        var snapshot = service.export();

        assertEquals(OnboardingStatus.COMPLETE, snapshot.onboardingStatus());
        assertEquals(1, snapshot.transformations().size());
        assertTrue(snapshot.experiments().isEmpty());
        assertTrue(snapshot.memoryProposals().isEmpty());
    }
}
