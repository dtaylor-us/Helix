package com.helix.api.wisdom.application;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.wisdom.adapter.out.persistence.WeeklyRetrospectiveRepository;
import com.helix.api.wisdom.domain.RetrospectiveSource;
import com.helix.api.wisdom.domain.WeeklyRetrospectiveEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class WeeklyRetrospectiveService {

    private static final int EXCERPT_LIMIT = 160;

    private final ReflectionService reflectionService;
    private final WeeklyRetrospectiveRepository repository;
    private final AiAssistantPort aiAssistantPort;

    public WeeklyRetrospectiveService(
        ReflectionService reflectionService, WeeklyRetrospectiveRepository repository, AiAssistantPort aiAssistantPort
    ) {
        this.reflectionService = reflectionService;
        this.repository = repository;
        this.aiAssistantPort = aiAssistantPort;
    }

    public WeeklyRetrospectiveDraft draft() {
        var end = OffsetDateTime.now(ZoneOffset.UTC);
        var start = end.minusDays(7);
        var reflections = reflectionService.recentSince(start);
        var summaries = reflections.stream().map(this::toReflectionSummary).toList();

        // An empty week is a real, distinct state, not something to hand to the AI to "explain" -
        // there's nothing to summarize. AI is only invoked (and required, per ADR-016) once there's
        // at least one reflection to narrate.
        if (reflections.isEmpty()) {
            return new WeeklyRetrospectiveDraft(
                start, end, summaries,
                "This week includes 0 reflections. Capture one small observation to restart momentum.",
                "Start with a two-minute reflection prompt: What was one small win this week?",
                RetrospectiveSource.DETERMINISTIC, null, null
            );
        }

        var aiSummary = aiAssistantPort.summarizeWeek(buildWeeklyContext(summaries));
        return new WeeklyRetrospectiveDraft(
            start, end, summaries, aiSummary.summary(), aiSummary.assistance(),
            aiSummary.deterministicFallback() ? RetrospectiveSource.DETERMINISTIC : RetrospectiveSource.AI,
            aiSummary.provider(), aiSummary.model()
        );
    }

    private String buildWeeklyContext(List<ReflectionSummary> summaries) {
        var context = new StringBuilder();
        context.append("Reflections from the past 7 days (").append(summaries.size()).append(" total):\n");
        for (var item : summaries) {
            context.append("- ").append(item.summary()).append("\n");
        }
        return context.toString();
    }

    @Transactional
    public WeeklyRetrospectiveEntity createSnapshot() {
        var draft = draft();
        var entity = new WeeklyRetrospectiveEntity(
            UUID.randomUUID(),
            draft.periodStart(),
            draft.periodEnd(),
            draft.summary(),
            draft.assistance(),
            OffsetDateTime.now(ZoneOffset.UTC),
            draft.source(),
            draft.aiProvider(),
            draft.aiModel()
        );
        return repository.save(entity);
    }

    public List<WeeklyRetrospectiveEntity> recentSnapshots() {
        return repository.findTop10ByOrderByCreatedAtDesc();
    }

    public WeeklyRetrospectiveEntity get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Retrospective not found"));
    }

    public List<WeeklyRetrospectiveEntity> search(String query) {
        return repository.findTop20BySummaryContainingIgnoreCaseOrderByCreatedAtDesc(query.trim());
    }

    private ReflectionSummary toReflectionSummary(ReflectionEntity reflection) {
        var content = reflection.getContent();
        var excerpt = content.length() <= EXCERPT_LIMIT ? content : content.substring(0, EXCERPT_LIMIT) + "...";
        return new ReflectionSummary(reflection.getId(), reflection.getCreatedAt(), excerpt);
    }

    public record WeeklyRetrospectiveDraft(
        OffsetDateTime periodStart,
        OffsetDateTime periodEnd,
        List<ReflectionSummary> reflectionSummaries,
        String summary,
        String assistance,
        RetrospectiveSource source,
        String aiProvider,
        String aiModel
    ) {}

    public record ReflectionSummary(UUID reflectionId, OffsetDateTime createdAt, String summary) {}
}
