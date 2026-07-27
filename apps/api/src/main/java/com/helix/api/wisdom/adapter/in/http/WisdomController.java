package com.helix.api.wisdom.adapter.in.http;

import com.helix.api.wisdom.application.WeeklyRetrospectiveService;
import com.helix.api.wisdom.application.WisdomService;
import com.helix.api.wisdom.domain.WisdomEntryEntity;
import com.helix.api.wisdom.domain.WisdomRevisionEntity;
import com.helix.api.wisdom.domain.WisdomSourceLinkEntity;
import com.helix.api.wisdom.domain.WisdomSourceType;
import com.helix.api.wisdom.domain.WeeklyRetrospectiveEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wisdom")
public class WisdomController {

    private final WeeklyRetrospectiveService retrospectiveService;
    private final WisdomService wisdomService;

    public WisdomController(WeeklyRetrospectiveService retrospectiveService, WisdomService wisdomService) {
        this.retrospectiveService = retrospectiveService;
        this.wisdomService = wisdomService;
    }

    @GetMapping("/weekly-retrospective")
    public WeeklyRetrospectiveDraftDto weeklyDraft() {
        var draft = retrospectiveService.draft();
        return new WeeklyRetrospectiveDraftDto(
            draft.periodStart().toString(),
            draft.periodEnd().toString(),
            draft.reflectionSummaries().stream()
                .map(item -> new ReflectionSummaryDto(item.reflectionId(), item.createdAt().toString(), item.summary()))
                .toList(),
            draft.summary(),
            draft.assistance()
        );
    }

    @PostMapping("/weekly-retrospective")
    public WeeklyRetrospectiveDto saveWeeklySnapshot() {
        return toRetrospectiveDto(retrospectiveService.createSnapshot());
    }

    @GetMapping("/retrospectives")
    public List<WeeklyRetrospectiveDto> retrospectives() {
        return retrospectiveService.recentSnapshots().stream().map(this::toRetrospectiveDto).toList();
    }

    @GetMapping
    public List<WisdomEntryDto> listWisdom() {
        return wisdomService.list().stream().map(this::toWisdomEntryDto).toList();
    }

    @PostMapping
    public WisdomEntryDto createWisdom(@Valid @RequestBody CreateWisdomRequest request) {
        var entry = wisdomService.create(
            request.statement(),
            request.retrospectiveId(),
            request.sources().stream()
                .map(item -> new WisdomService.WisdomSourceInput(item.sourceType(), item.sourceRecordId(), item.note()))
                .toList()
        );
        return toWisdomEntryDto(entry);
    }

    @GetMapping("/{id}")
    public WisdomDetailDto detail(@PathVariable UUID id) {
        var entry = wisdomService.get(id);
        var revisions = wisdomService.revisionHistory(id).stream().map(this::toRevisionDto).toList();
        var sources = wisdomService.sources(id).stream().map(this::toSourceDto).toList();
        return new WisdomDetailDto(toWisdomEntryDto(entry), revisions, sources);
    }

    @PostMapping("/{id}/revisions")
    public WisdomRevisionDto revise(@PathVariable UUID id, @Valid @RequestBody ReviseWisdomRequest request) {
        return toRevisionDto(wisdomService.revise(id, request.statement(), request.reason()));
    }

    private WeeklyRetrospectiveDto toRetrospectiveDto(WeeklyRetrospectiveEntity entity) {
        return new WeeklyRetrospectiveDto(
            entity.getId(),
            entity.getPeriodStart().toString(),
            entity.getPeriodEnd().toString(),
            entity.getSummary(),
            entity.getAssistance(),
            entity.getCreatedAt().toString()
        );
    }

    private WisdomEntryDto toWisdomEntryDto(WisdomEntryEntity entity) {
        return new WisdomEntryDto(
            entity.getId(),
            entity.getStatement(),
            entity.getStatus().name(),
            entity.getRetrospectiveId(),
            entity.getCreatedAt().toString(),
            entity.getRevisedAt().toString()
        );
    }

    private WisdomRevisionDto toRevisionDto(WisdomRevisionEntity entity) {
        return new WisdomRevisionDto(
            entity.getId(),
            entity.getWisdomId(),
            entity.getPreviousStatement(),
            entity.getNewStatement(),
            entity.getReason(),
            entity.getCreatedAt().toString()
        );
    }

    private WisdomSourceDto toSourceDto(WisdomSourceLinkEntity entity) {
        return new WisdomSourceDto(
            entity.getId(),
            entity.getWisdomId(),
            entity.getSourceType().name(),
            entity.getSourceRecordId(),
            entity.getNote(),
            entity.getCreatedAt().toString()
        );
    }

    public record WeeklyRetrospectiveDraftDto(String periodStart, String periodEnd,
                                              List<ReflectionSummaryDto> reflectionSummaries,
                                              String summary, String assistance) {}

    public record ReflectionSummaryDto(UUID reflectionId, String createdAt, String summary) {}

    public record WeeklyRetrospectiveDto(UUID id, String periodStart, String periodEnd,
                                         String summary, String assistance, String createdAt) {}

    public record CreateWisdomRequest(@NotBlank @Size(max = 2000) String statement,
                                      UUID retrospectiveId,
                                      @NotEmpty List<@Valid SourceLinkRequest> sources) {}

    public record SourceLinkRequest(@NotNull WisdomSourceType sourceType,
                                    @NotNull UUID sourceRecordId,
                                    @Size(max = 600) String note) {}

    public record ReviseWisdomRequest(@NotBlank @Size(max = 2000) String statement,
                                      @NotBlank @Size(max = 1000) String reason) {}

    public record WisdomEntryDto(UUID id, String statement, String status,
                                 UUID retrospectiveId, String createdAt, String revisedAt) {}

    public record WisdomDetailDto(WisdomEntryDto wisdom,
                                  List<WisdomRevisionDto> revisions,
                                  List<WisdomSourceDto> sources) {}

    public record WisdomRevisionDto(UUID id, UUID wisdomId, String previousStatement,
                                    String newStatement, String reason, String createdAt) {}

    public record WisdomSourceDto(UUID id, UUID wisdomId, String sourceType,
                                  UUID sourceRecordId, String note, String createdAt) {}
}
