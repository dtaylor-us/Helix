package com.helix.api.reflection.application;

import com.helix.api.experiments.application.ExperimentService;
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

    public ReflectionService(ReflectionRepository repository, ExperimentService experimentService, SuggestionService suggestionService) {
        this.repository = repository;
        this.experimentService = experimentService;
        this.suggestionService = suggestionService;
    }

    @Transactional
    public ReflectionWithSuggestion create(UUID experimentId, String content) {
        return create(experimentId, content, null, null, null, null);
    }

    @Transactional
    public ReflectionWithSuggestion create(
        UUID experimentId, String content, Boolean attempted, String noticed, String evidenceNoted, String surprise
    ) {
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
            OffsetDateTime.now()
        ));
        int previousAttempts = repository.findByExperimentIdOrderByCreatedAtDesc(experimentId).size();
        var suggestion = suggestionService.createDeterministic(experimentId, reflection.getId(), experiment.getNextAction(), previousAttempts);
        return new ReflectionWithSuggestion(reflection, suggestion);
    }

    public ReflectionEntity get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Reflection not found"));
    }

    public List<ReflectionEntity> history(UUID experimentId) {
        return repository.findByExperimentIdOrderByCreatedAtDesc(experimentId);
    }

    public List<ReflectionEntity> listForRetrieval() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public List<ReflectionEntity> recentSince(OffsetDateTime threshold) {
        return repository.findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(threshold);
    }

    public List<ReflectionEntity> search(String query) {
        return repository.findTop20ByContentContainingIgnoreCaseOrderByCreatedAtDesc(query.trim());
    }

    public record ReflectionWithSuggestion(ReflectionEntity reflection, SuggestionEntity suggestion) {}
}
