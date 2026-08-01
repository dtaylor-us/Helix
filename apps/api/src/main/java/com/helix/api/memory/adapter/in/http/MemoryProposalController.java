package com.helix.api.memory.adapter.in.http;

import com.helix.api.memory.application.MemoryProposalService;
import com.helix.api.memory.domain.MemoryProposalEntity;
import com.helix.api.memory.domain.MemoryProposalRevisionEntity;
import com.helix.api.memory.domain.MemoryProposalStatus;
import com.helix.api.memory.domain.MemorySourceKind;
import com.helix.api.memory.domain.MemorySourceRecordType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memory/proposals")
public class MemoryProposalController {

    private final MemoryProposalService service;

    public MemoryProposalController(MemoryProposalService service) {
        this.service = service;
    }

    @GetMapping
    public List<MemoryProposalDto> list() {
        return service.list().stream().map(this::toDto).toList();
    }

    @PostMapping
    public MemoryProposalDto create(@Valid @RequestBody CreateMemoryProposalRequest request) {
        return toDto(service.create(
            request.statement(),
            request.sourceKind(),
            request.sourceRecordType(),
            request.sourceRecordId(),
            request.sourceExcerpt()
        ));
    }

    /**
     * Propose an AI-derived candidate memory statement from a reflection. Nothing is persisted —
     * the response is meant for the user to review, edit, and submit via {@link #create} (with
     * sourceKind AI_DERIVED / sourceRecordType REFLECTION), per ADR-008's explicit-review
     * requirement for AI-derived content. See ADR-018.
     */
    @PostMapping("/draft")
    public MemoryProposalDraftDto proposeDraft(@Valid @RequestBody ProposeMemoryDraftRequest request) {
        var draft = service.proposeFromReflection(request.reflectionId());
        return new MemoryProposalDraftDto(draft.statement(), draft.source(), draft.aiProvider(), draft.aiModel());
    }

    @GetMapping("/{id}")
    public MemoryProposalDetailDto detail(@PathVariable UUID id) {
        var proposal = service.get(id);
        var revisions = service.revisions(id).stream().map(this::toRevisionDto).toList();
        return new MemoryProposalDetailDto(toDto(proposal), revisions);
    }

    @PostMapping("/{id}/revise")
    public MemoryProposalRevisionDto revise(@PathVariable UUID id, @Valid @RequestBody ReviseMemoryProposalRequest request) {
        return toRevisionDto(service.revise(id, request.statement(), request.reason(), request.sourceExcerpt()));
    }

    @PostMapping("/{id}/accept")
    public MemoryProposalRevisionDto accept(@PathVariable UUID id, @Valid @RequestBody ReviewMemoryProposalRequest request) {
        return toRevisionDto(service.accept(id, request.reason()));
    }

    @PostMapping("/{id}/reject")
    public MemoryProposalRevisionDto reject(@PathVariable UUID id, @Valid @RequestBody ReviewMemoryProposalRequest request) {
        return toRevisionDto(service.reject(id, request.reason()));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    private MemoryProposalDto toDto(MemoryProposalEntity entity) {
        return new MemoryProposalDto(
            entity.getId(),
            entity.getStatement(),
            entity.getStatus().name(),
            entity.getSourceKind().name(),
            entity.getSourceRecordType().name(),
            entity.getSourceRecordId(),
            entity.getSourceExcerpt(),
            entity.getCreatedAt().toString(),
            entity.getRevisedAt().toString()
        );
    }

    private MemoryProposalRevisionDto toRevisionDto(MemoryProposalRevisionEntity entity) {
        return new MemoryProposalRevisionDto(
            entity.getId(),
            entity.getMemoryProposalId(),
            entity.getPreviousStatement(),
            entity.getNewStatement(),
            entity.getPreviousStatus().name(),
            entity.getNewStatus().name(),
            entity.getReason(),
            entity.getCreatedAt().toString()
        );
    }

    public record ProposeMemoryDraftRequest(@NotNull UUID reflectionId) {}

    public record MemoryProposalDraftDto(String statement, String source, String aiProvider, String aiModel) {}

    public record CreateMemoryProposalRequest(@NotBlank @Size(max = 2000) String statement,
                                              @NotNull MemorySourceKind sourceKind,
                                              @NotNull MemorySourceRecordType sourceRecordType,
                                              @NotNull UUID sourceRecordId,
                                              @Size(max = 2000) String sourceExcerpt) {}

    public record ReviseMemoryProposalRequest(@NotBlank @Size(max = 2000) String statement,
                                              @NotBlank @Size(max = 1000) String reason,
                                              @Size(max = 2000) String sourceExcerpt) {}

    public record ReviewMemoryProposalRequest(@NotBlank @Size(max = 1000) String reason) {}

    public record MemoryProposalDto(UUID id, String statement, String status, String sourceKind,
                                    String sourceRecordType, UUID sourceRecordId, String sourceExcerpt,
                                    String createdAt, String revisedAt) {}

    public record MemoryProposalDetailDto(MemoryProposalDto proposal, List<MemoryProposalRevisionDto> revisions) {}

    public record MemoryProposalRevisionDto(UUID id, UUID memoryProposalId, String previousStatement,
                                            String newStatement, String previousStatus, String newStatus,
                                            String reason, String createdAt) {}
}