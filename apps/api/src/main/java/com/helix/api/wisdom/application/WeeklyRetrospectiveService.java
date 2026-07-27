package com.helix.api.wisdom.application;

import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.wisdom.adapter.out.persistence.WeeklyRetrospectiveRepository;
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

    public WeeklyRetrospectiveService(ReflectionService reflectionService, WeeklyRetrospectiveRepository repository) {
        this.reflectionService = reflectionService;
        this.repository = repository;
    }

    public WeeklyRetrospectiveDraft draft() {
        var end = OffsetDateTime.now(ZoneOffset.UTC);
        var start = end.minusDays(7);
        var reflections = reflectionService.recentSince(start);
        var summaries = reflections.stream().map(this::toReflectionSummary).toList();
        long denseEntries = reflections.stream().filter(item -> item.getContent().length() > 120).count();

        var summary = "This week includes " + reflections.size() + " reflections. " +
            (reflections.isEmpty()
                ? "Capture one small observation to restart momentum."
                : "Most notes emphasize steady practice over intensity.");

        var assistance = reflections.isEmpty()
            ? "Start with a two-minute reflection prompt: What was one small win this week?"
            : "Choose one recurring pattern and run a smaller experiment next week. " +
              "Detailed reflections this week: " + denseEntries + ".";

        return new WeeklyRetrospectiveDraft(start, end, summaries, summary, assistance);
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
            OffsetDateTime.now(ZoneOffset.UTC)
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
        String assistance
    ) {}

    public record ReflectionSummary(UUID reflectionId, OffsetDateTime createdAt, String summary) {}
}
