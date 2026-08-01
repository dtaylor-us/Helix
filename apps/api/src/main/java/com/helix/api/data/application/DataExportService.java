package com.helix.api.data.application;

import com.helix.api.beliefs.adapter.out.persistence.BeliefRepository;
import com.helix.api.beliefs.adapter.out.persistence.BeliefRevisionRepository;
import com.helix.api.beliefs.domain.BeliefEntity;
import com.helix.api.beliefs.domain.BeliefRevisionEntity;
import com.helix.api.evidence.adapter.out.persistence.EvidenceRepository;
import com.helix.api.evidence.domain.EvidenceEntity;
import com.helix.api.experiments.adapter.out.persistence.ExperimentRepository;
import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.memory.adapter.out.persistence.MemoryProposalRepository;
import com.helix.api.memory.adapter.out.persistence.MemoryProposalRevisionRepository;
import com.helix.api.memory.domain.MemoryProposalEntity;
import com.helix.api.memory.domain.MemoryProposalRevisionEntity;
import com.helix.api.onboarding.application.OnboardingService;
import com.helix.api.onboarding.domain.OnboardingStatus;
import com.helix.api.reflection.adapter.out.persistence.ReflectionRepository;
import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.suggestions.adapter.out.persistence.SuggestionRepository;
import com.helix.api.suggestions.domain.SuggestionEntity;
import com.helix.api.transformation.adapter.out.persistence.TransformationRepository;
import com.helix.api.transformation.domain.TransformationEntity;
import com.helix.api.wisdom.adapter.out.persistence.WeeklyRetrospectiveRepository;
import com.helix.api.wisdom.adapter.out.persistence.WisdomEntryRepository;
import com.helix.api.wisdom.adapter.out.persistence.WisdomRevisionRepository;
import com.helix.api.wisdom.adapter.out.persistence.WisdomSourceLinkRepository;
import com.helix.api.wisdom.domain.WeeklyRetrospectiveEntity;
import com.helix.api.wisdom.domain.WisdomEntryEntity;
import com.helix.api.wisdom.domain.WisdomRevisionEntity;
import com.helix.api.wisdom.domain.WisdomSourceLinkEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Phase 9 (ADR-019): a complete, human-readable export of every user-owned record in the app.
 * Deliberately excludes {@code semantic_search_documents} — that table holds embeddings derived
 * from other records' text, not user-authored content, and is regenerable via the existing
 * {@code POST /api/v1/search/index/rebuild} endpoint.
 */
@Service
public class DataExportService {

    private final TransformationRepository transformationRepository;
    private final ExperimentRepository experimentRepository;
    private final ReflectionRepository reflectionRepository;
    private final SuggestionRepository suggestionRepository;
    private final BeliefRepository beliefRepository;
    private final BeliefRevisionRepository beliefRevisionRepository;
    private final EvidenceRepository evidenceRepository;
    private final WeeklyRetrospectiveRepository weeklyRetrospectiveRepository;
    private final WisdomEntryRepository wisdomEntryRepository;
    private final WisdomRevisionRepository wisdomRevisionRepository;
    private final WisdomSourceLinkRepository wisdomSourceLinkRepository;
    private final MemoryProposalRepository memoryProposalRepository;
    private final MemoryProposalRevisionRepository memoryProposalRevisionRepository;
    private final OnboardingService onboardingService;

    public DataExportService(
        TransformationRepository transformationRepository, ExperimentRepository experimentRepository,
        ReflectionRepository reflectionRepository, SuggestionRepository suggestionRepository,
        BeliefRepository beliefRepository, BeliefRevisionRepository beliefRevisionRepository,
        EvidenceRepository evidenceRepository, WeeklyRetrospectiveRepository weeklyRetrospectiveRepository,
        WisdomEntryRepository wisdomEntryRepository, WisdomRevisionRepository wisdomRevisionRepository,
        WisdomSourceLinkRepository wisdomSourceLinkRepository, MemoryProposalRepository memoryProposalRepository,
        MemoryProposalRevisionRepository memoryProposalRevisionRepository, OnboardingService onboardingService
    ) {
        this.transformationRepository = transformationRepository;
        this.experimentRepository = experimentRepository;
        this.reflectionRepository = reflectionRepository;
        this.suggestionRepository = suggestionRepository;
        this.beliefRepository = beliefRepository;
        this.beliefRevisionRepository = beliefRevisionRepository;
        this.evidenceRepository = evidenceRepository;
        this.weeklyRetrospectiveRepository = weeklyRetrospectiveRepository;
        this.wisdomEntryRepository = wisdomEntryRepository;
        this.wisdomRevisionRepository = wisdomRevisionRepository;
        this.wisdomSourceLinkRepository = wisdomSourceLinkRepository;
        this.memoryProposalRepository = memoryProposalRepository;
        this.memoryProposalRevisionRepository = memoryProposalRevisionRepository;
        this.onboardingService = onboardingService;
    }

    public DataExportSnapshot export() {
        return new DataExportSnapshot(
            onboardingService.get().getStatus(),
            transformationRepository.findAll(),
            experimentRepository.findAll(),
            reflectionRepository.findAll(),
            suggestionRepository.findAll(),
            beliefRepository.findAll(),
            beliefRevisionRepository.findAll(),
            evidenceRepository.findAll(),
            weeklyRetrospectiveRepository.findAll(),
            wisdomEntryRepository.findAll(),
            wisdomRevisionRepository.findAll(),
            wisdomSourceLinkRepository.findAll(),
            memoryProposalRepository.findAll(),
            memoryProposalRevisionRepository.findAll()
        );
    }

    public record DataExportSnapshot(
        OnboardingStatus onboardingStatus,
        List<TransformationEntity> transformations,
        List<ExperimentEntity> experiments,
        List<ReflectionEntity> reflections,
        List<SuggestionEntity> suggestions,
        List<BeliefEntity> beliefs,
        List<BeliefRevisionEntity> beliefRevisions,
        List<EvidenceEntity> evidence,
        List<WeeklyRetrospectiveEntity> weeklyRetrospectives,
        List<WisdomEntryEntity> wisdomEntries,
        List<WisdomRevisionEntity> wisdomRevisions,
        List<WisdomSourceLinkEntity> wisdomSourceLinks,
        List<MemoryProposalEntity> memoryProposals,
        List<MemoryProposalRevisionEntity> memoryProposalRevisions
    ) {}
}
