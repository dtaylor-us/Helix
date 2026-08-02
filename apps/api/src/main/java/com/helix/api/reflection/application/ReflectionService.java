package com.helix.api.reflection.application;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.experiments.application.ExperimentService;
import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.reflection.adapter.out.persistence.ReflectionRepository;
import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.suggestions.application.SuggestionService;
import com.helix.api.suggestions.domain.SuggestionEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ReflectionService {

    private final ReflectionRepository repository;
    private final ExperimentService experimentService;
    private final SuggestionService suggestionService;
    private final AiAssistantPort aiAssistantPort;
    private final CurrentUserProvider currentUserProvider;

    public ReflectionService(
        ReflectionRepository repository, ExperimentService experimentService,
        SuggestionService suggestionService, AiAssistantPort aiAssistantPort, CurrentUserProvider currentUserProvider
    ) {
        this.repository = repository;
        this.experimentService = experimentService;
        this.suggestionService = suggestionService;
        this.aiAssistantPort = aiAssistantPort;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public ReflectionWithSuggestion create(UUID experimentId, String content) {
        return create(experimentId, content, null, null, null, null);
    }

    @Transactional
    public ReflectionWithSuggestion create(
        UUID experimentId, String content, Boolean attempted, String noticed, String evidenceNoted, String surprise
    ) {
        // experimentService.get() 404s if experimentId doesn't belong to the caller.
        var experiment = experimentService.get(experimentId);

        var normalizedNoticed = noticed == null || noticed.isBlank() ? null : noticed.trim();
        var normalizedEvidenceNoted = evidenceNoted == null || evidenceNoted.isBlank() ? null : evidenceNoted.trim();
        var normalizedSurprise = surprise == null || surprise.isBlank() ? null : surprise.trim();

        var reflection = repository.save(new ReflectionEntity(
            UUID.randomUUID(),
            experimentId,
            content.trim(),
            attempted,
            normalizedNoticed,
            normalizedEvidenceNoted,
            normalizedSurprise,
            OffsetDateTime.now(),
            currentUserProvider.currentUserId()
        ));
        int previousAttempts = Math.max(0, repository.findByExperimentIdOrderByCreatedAtDesc(experimentId).size() - 1);

        // AI is the required content source for post-reflection suggestions (ADR-016); the
        // AiAssistantPort adapter itself handles provider selection and outage fallback, and
        // reports whether the returned text is a live model answer or a fallback so we can record
        // accurate provenance below.
        var aiSuggestion = aiAssistantPort.suggestNextAction(buildSuggestionContext(experiment, reflection, previousAttempts));
        var suggestion = suggestionService.createFromAi(
            experimentId, reflection.getId(), aiSuggestion.text(), aiSuggestion.provider(), aiSuggestion.model(),
            aiSuggestion.deterministicFallback()
        );
        return new ReflectionWithSuggestion(reflection, suggestion);
    }

    private String buildSuggestionContext(ExperimentEntity experiment, ReflectionEntity reflection, int previousAttempts) {
        var context = new StringBuilder();
        context.append("Experiment: ").append(experiment.getTitle()).append(". ");
        if (experiment.getHypothesis() != null && !experiment.getHypothesis().isBlank()) {
            context.append("Hypothesis: ").append(experiment.getHypothesis()).append(". ");
        }
        if (experiment.getNextAction() != null && !experiment.getNextAction().isBlank()) {
            context.append("Previously planned next action: ").append(experiment.getNextAction()).append(". ");
        }
        context.append("Latest reflection: ").append(reflection.getContent()).append(". ");
        if (reflection.getNoticed() != null) {
            context.append("Noticed: ").append(reflection.getNoticed()).append(". ");
        }
        if (reflection.getEvidenceNoted() != null) {
            context.append("Evidence: ").append(reflection.getEvidenceNoted()).append(". ");
        }
        if (reflection.getSurprise() != null) {
            context.append("Surprise: ").append(reflection.getSurprise()).append(". ");
        }
        context.append("Number of previous reflections on this experiment: ").append(previousAttempts).append(".");
        return context.toString();
    }

    public ReflectionEntity get(UUID id) {
        return repository.findByIdAndOwnerId(id, currentUserProvider.currentUserId())
            .orElseThrow(() -> new NoSuchElementException("Reflection not found"));
    }

    public List<ReflectionEntity> history(UUID experimentId) {
        // experimentId is expected to already be ownership-checked by the caller (it was resolved
        // via ExperimentService.get() to get here) -- listing by experimentId alone is safe.
        return repository.findByExperimentIdOrderByCreatedAtDesc(experimentId);
    }

    public List<ReflectionEntity> listForRetrieval() {
        return repository.findByOwnerIdOrderByCreatedAtDesc(currentUserProvider.currentUserId());
    }

    public List<ReflectionEntity> recentSince(OffsetDateTime threshold) {
        return repository.findByOwnerIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(currentUserProvider.currentUserId(), threshold);
    }

    public List<ReflectionEntity> search(String query) {
        return repository.findTop20ByOwnerIdAndContentContainingIgnoreCaseOrderByCreatedAtDesc(
            currentUserProvider.currentUserId(), query.trim());
    }

    public record ReflectionWithSuggestion(ReflectionEntity reflection, SuggestionEntity suggestion) {}
}
