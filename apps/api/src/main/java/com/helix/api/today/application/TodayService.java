package com.helix.api.today.application;

import com.helix.api.experiments.application.ExperimentService;
import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.suggestions.application.SuggestionService;
import com.helix.api.suggestions.domain.SuggestionEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TodayService {

    private final ExperimentService experimentService;
    private final ReflectionService reflectionService;
    private final SuggestionService suggestionService;

    public TodayService(ExperimentService experimentService, ReflectionService reflectionService, SuggestionService suggestionService) {
        this.experimentService = experimentService;
        this.reflectionService = reflectionService;
        this.suggestionService = suggestionService;
    }

    public Optional<TodaySnapshot> snapshot() {
        return experimentService.activeExperiment().map(experiment -> {
            List<ReflectionEntity> reflections = reflectionService.history(experiment.getId());
            List<SuggestionEntity> suggestions = suggestionService.history(experiment.getId());
            return new TodaySnapshot(experiment, reflections, suggestions);
        });
    }

    public record TodaySnapshot(
        ExperimentEntity activeExperiment,
        List<ReflectionEntity> reflectionHistory,
        List<SuggestionEntity> suggestionHistory
    ) {}
}
