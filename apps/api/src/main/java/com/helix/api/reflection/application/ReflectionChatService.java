package com.helix.api.reflection.application;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.experiments.application.ExperimentService;
import com.helix.api.experiments.domain.ExperimentEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReflectionChatService {

    private final ExperimentService experimentService;
    private final AiAssistantPort aiAssistantPort;

    public ReflectionChatService(ExperimentService experimentService, AiAssistantPort aiAssistantPort) {
        this.experimentService = experimentService;
        this.aiAssistantPort = aiAssistantPort;
    }

    public AiAssistantPort.AiSuggestion nextTurn(UUID experimentId, List<ChatMessage> transcript) {
        var experiment = experimentService.get(experimentId);
        return aiAssistantPort.continueReflectionChat(buildContext(experiment, transcript));
    }

    public AiAssistantPort.AiReflectionStructure finish(UUID experimentId, List<ChatMessage> transcript) {
        var experiment = experimentService.get(experimentId);
        return aiAssistantPort.structureReflection(buildContext(experiment, transcript));
    }

    private String buildContext(ExperimentEntity experiment, List<ChatMessage> transcript) {
        var context = new StringBuilder();
        context.append("Experiment: ").append(experiment.getTitle()).append("\n");
        if (experiment.getHypothesis() != null && !experiment.getHypothesis().isBlank()) {
            context.append("Hypothesis: ").append(experiment.getHypothesis()).append("\n");
        }
        if (experiment.getNextAction() != null && !experiment.getNextAction().isBlank()) {
            context.append("Planned next action: ").append(experiment.getNextAction()).append("\n");
        }
        context.append("Conversation transcript:\n");
        for (ChatMessage message : transcript) {
            String roleLabel = "assistant".equals(message.role()) ? "Assistant" : "User";
            context.append(roleLabel).append(": ").append(message.text()).append("\n");
        }
        return context.toString();
    }

    public record ChatMessage(String role, String text) {}
}
