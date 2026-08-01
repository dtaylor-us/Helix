package com.helix.api.data.application;

import com.helix.api.beliefs.adapter.out.persistence.BeliefRepository;
import com.helix.api.beliefs.adapter.out.persistence.BeliefRevisionRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 9 (ADR-019): a whole-app, hard-delete data wipe. There is no per-user scoping today (ADR-013
 * defers auth), so this deletes every record in every module and resets onboarding back to
 * NOT_STARTED. Irreversible — no soft-delete/tombstone, no undo. Deletes leaf tables before their
 * parents even though most foreign keys already cascade, as a deliberate belt-and-suspenders
 * ordering rather than relying solely on cascade configuration.
 */
@Service
public class DataDeletionService {

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
    private final SemanticSearchDocumentRepository semanticSearchDocumentRepository;
    private final OnboardingService onboardingService;

    public DataDeletionService(
        TransformationRepository transformationRepository, ExperimentRepository experimentRepository,
        ReflectionRepository reflectionRepository, SuggestionRepository suggestionRepository,
        BeliefRepository beliefRepository, BeliefRevisionRepository beliefRevisionRepository,
        EvidenceRepository evidenceRepository, WeeklyRetrospectiveRepository weeklyRetrospectiveRepository,
        WisdomEntryRepository wisdomEntryRepository, WisdomRevisionRepository wisdomRevisionRepository,
        WisdomSourceLinkRepository wisdomSourceLinkRepository, MemoryProposalRepository memoryProposalRepository,
        MemoryProposalRevisionRepository memoryProposalRevisionRepository,
        SemanticSearchDocumentRepository semanticSearchDocumentRepository, OnboardingService onboardingService
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
        this.semanticSearchDocumentRepository = semanticSearchDocumentRepository;
        this.onboardingService = onboardingService;
    }

    @Transactional
    public void deleteEverything() {
        semanticSearchDocumentRepository.deleteAllInBatch();

        memoryProposalRevisionRepository.deleteAllInBatch();
        memoryProposalRepository.deleteAllInBatch();

        wisdomRevisionRepository.deleteAllInBatch();
        wisdomSourceLinkRepository.deleteAllInBatch();
        wisdomEntryRepository.deleteAllInBatch();
        weeklyRetrospectiveRepository.deleteAllInBatch();

        beliefRevisionRepository.deleteAllInBatch();
        evidenceRepository.deleteAllInBatch();
        beliefRepository.deleteAllInBatch();

        suggestionRepository.deleteAllInBatch();
        reflectionRepository.deleteAllInBatch();
        experimentRepository.deleteAllInBatch();
        transformationRepository.deleteAllInBatch();

        onboardingService.reset();
    }
}
