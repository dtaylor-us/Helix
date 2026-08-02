package com.helix.api.experiments.application;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.experiments.adapter.out.persistence.ExperimentRepository;
import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.experiments.domain.ExperimentStatus;
import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.onboarding.application.OnboardingService;
import com.helix.api.transformation.application.TransformationService;
import com.helix.api.transformation.domain.TransformationEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExperimentService {

    private final ExperimentRepository repository;
    private final TransformationService transformationService;
    private final AiAssistantPort aiAssistantPort;
    private final OnboardingService onboardingService;
    private final CurrentUserProvider currentUserProvider;

    public ExperimentService(
        ExperimentRepository repository, TransformationService transformationService,
        AiAssistantPort aiAssistantPort, OnboardingService onboardingService, CurrentUserProvider currentUserProvider
    ) {
        this.repository = repository;
        this.transformationService = transformationService;
        this.aiAssistantPort = aiAssistantPort;
        this.onboardingService = onboardingService;
        this.currentUserProvider = currentUserProvider;
    }

    public ExperimentEntity create(UUID transformationId, String title, String hypothesis, String nextAction) {
        return create(transformationId, title, hypothesis, nextAction, null, null, null);
    }

    public ExperimentEntity create(
        UUID transformationId, String title, String hypothesis, String nextAction,
        String cadence, String evidenceOfSuccess, LocalDate reviewAt
    ) {
        // transformationService.get() 404s if transformationId doesn't belong to the caller -- this
        // is what stops someone from creating an experiment under a transformation they don't own.
        transformationService.get(transformationId);
        var entity = new ExperimentEntity(
            UUID.randomUUID(),
            transformationId,
            title.trim(),
            hypothesis,
            nextAction,
            cadence,
            evidenceOfSuccess,
            reviewAt,
            ExperimentStatus.ACTIVE,
            OffsetDateTime.now(),
            currentUserProvider.currentUserId()
        );
        var saved = repository.save(entity);
        // Phase 7: server-persisted onboarding state. No-op once onboarding is already COMPLETE.
        onboardingService.advanceToComplete();
        return saved;
    }

    public ExperimentEntity get(UUID id) {
        return repository.findByIdAndOwnerId(id, currentUserProvider.currentUserId())
            .orElseThrow(() -> new NoSuchElementException("Experiment not found"));
    }

    /**
     * Called when a user accepts (or replaces) a "Suggested Small Action" for this experiment on the
     * Today page, so that commitment becomes visible on the experiment/journey itself rather than
     * only existing as a status flag on an isolated suggestion row. See {@code SuggestionService}.
     */
    public ExperimentEntity reviseNextAction(UUID id, String nextAction) {
        var experiment = get(id);
        experiment.reviseNextAction(nextAction);
        return repository.save(experiment);
    }

    public Optional<ExperimentEntity> activeExperiment() {
        return repository.findFirstByOwnerIdAndStatusOrderByCreatedAtDesc(currentUserProvider.currentUserId(), ExperimentStatus.ACTIVE);
    }

    /**
     * Propose a draft experiment for a transformation using AI (ADR-016, Phase 5 slice C). Nothing
     * is persisted by this call — per ADR-008, the caller must route the result through the normal
     * explicit-review/accept flow ({@link #create}) before anything becomes a real experiment.
     */
    public ExperimentDraft proposeDraft(UUID transformationId) {
        var transformation = transformationService.get(transformationId);
        var aiDraft = aiAssistantPort.proposeExperiment(buildDraftContext(transformation));
        return new ExperimentDraft(
            aiDraft.title(), aiDraft.hypothesis(), aiDraft.nextAction(), aiDraft.cadence(), aiDraft.evidenceOfSuccess(),
            aiDraft.deterministicFallback() ? "DETERMINISTIC" : "AI", aiDraft.provider(), aiDraft.model()
        );
    }

    private String buildDraftContext(TransformationEntity transformation) {
        var context = new StringBuilder();
        context.append("Transformation: ").append(transformation.getTitle()).append(". ");
        if (transformation.getPurpose() != null && !transformation.getPurpose().isBlank()) {
            context.append("Purpose: ").append(transformation.getPurpose()).append(". ");
        }
        if (transformation.getDesiredIdentity() != null && !transformation.getDesiredIdentity().isBlank()) {
            context.append("Who they're becoming: ").append(transformation.getDesiredIdentity()).append(". ");
        }
        if (transformation.getObstacle() != null && !transformation.getObstacle().isBlank()) {
            context.append("What gets in the way: ").append(transformation.getObstacle()).append(". ");
        }
        return context.toString();
    }

    public record ExperimentDraft(
        String title, String hypothesis, String nextAction, String cadence, String evidenceOfSuccess,
        String source, String aiProvider, String aiModel
    ) {}
}
