package com.helix.api.beliefs.adapter.in.http;

import com.helix.api.beliefs.application.BeliefService;
import com.helix.api.beliefs.domain.BeliefEntity;
import com.helix.api.beliefs.domain.BeliefRevisionEntity;
import com.helix.api.beliefs.domain.BeliefType;
import com.helix.api.evidence.application.EvidenceService;
import com.helix.api.evidence.domain.EvidenceDirection;
import com.helix.api.evidence.domain.EvidenceEntity;
import com.helix.api.evidence.domain.ProvenanceRecordType;
import com.helix.api.evidence.domain.ProvenanceSourceKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/v1/beliefs")
public class BeliefController {

    private final BeliefService beliefService;
    private final EvidenceService evidenceService;

    public BeliefController(BeliefService beliefService, EvidenceService evidenceService) {
        this.beliefService = beliefService;
        this.evidenceService = evidenceService;
    }

    @PostMapping
    public BeliefDto create(@Valid @RequestBody CreateBeliefRequest request) {
        return toBeliefDto(beliefService.create(request.transformationId(), request.statement(), request.type()));
    }

    @GetMapping
    public List<BeliefDto> list() {
        return beliefService.list().stream().map(this::toBeliefDto).toList();
    }

    @GetMapping("/{id}")
    public BeliefDetailDto get(@PathVariable UUID id) {
        var belief = beliefService.get(id);
        var revisions = beliefService.revisionHistory(id).stream().map(this::toRevisionDto).toList();
        var timeline = evidenceService.timeline(id).stream().map(this::toEvidenceDto).toList();
        return new BeliefDetailDto(beliefSummary(belief), revisions, timeline, buildNarrative(belief, timeline, revisions));
    }

    @PostMapping("/{id}/revisions")
    public BeliefRevisionDto revise(@PathVariable UUID id, @Valid @RequestBody ReviseBeliefRequest request) {
        return toRevisionDto(beliefService.revise(id, request.statement(), request.type(), request.reason(), request.sourceEvidenceId()));
    }

    @PostMapping("/{id}/evidence")
    public EvidenceDto createEvidence(@PathVariable UUID id, @Valid @RequestBody CreateEvidenceRequest request) {
        var provenance = request.provenance();
        return toEvidenceDto(evidenceService.create(
            id,
            request.experimentId(),
            request.reflectionId(),
            request.summary(),
            request.interpretation(),
            request.direction(),
            provenance.sourceKind(),
            provenance.recordType(),
            provenance.recordId(),
            provenance.excerpt()
        ));
    }

    private BeliefDto beliefSummary(BeliefEntity entity) {
        return toBeliefDto(entity);
    }

    private BeliefDto toBeliefDto(BeliefEntity entity) {
        return new BeliefDto(
            entity.getId(),
            entity.getTransformationId(),
            entity.getStatement(),
            entity.getType().name(),
            entity.getCreatedAt().toString(),
            entity.getRevisedAt().toString()
        );
    }

    private BeliefRevisionDto toRevisionDto(BeliefRevisionEntity entity) {
        return new BeliefRevisionDto(
            entity.getId(),
            entity.getBeliefId(),
            entity.getPreviousStatement(),
            entity.getNewStatement(),
            entity.getPreviousType().name(),
            entity.getNewType().name(),
            entity.getReason(),
            entity.getSourceEvidenceId(),
            entity.getCreatedAt().toString()
        );
    }

    private EvidenceDto toEvidenceDto(EvidenceEntity entity) {
        return new EvidenceDto(
            entity.getId(),
            entity.getBeliefId(),
            entity.getExperimentId(),
            entity.getReflectionId(),
            entity.getSummary(),
            entity.getInterpretation(),
            entity.getDirection().name(),
            new ProvenanceDto(
                entity.getProvenanceSourceKind().name(),
                entity.getProvenanceRecordType().name(),
                entity.getProvenanceRecordId(),
                entity.getProvenanceExcerpt()
            ),
            entity.getCreatedAt().toString()
        );
    }

    private String buildNarrative(BeliefEntity belief, List<EvidenceDto> timeline, List<BeliefRevisionDto> revisions) {
        long supports = timeline.stream().filter(item -> item.direction().equals(EvidenceDirection.SUPPORTS.name())).count();
        long challenges = timeline.stream().filter(item -> item.direction().equals(EvidenceDirection.CHALLENGES.name())).count();
        if (revisions.isEmpty()) {
            return "This belief is currently tracked as " + belief.getType().name().toLowerCase() +
                " with " + supports + " supporting and " + challenges + " challenging evidence entries.";
        }
        var latest = revisions.get(0);
        return "This belief was last revised because '" + latest.reason() + "'. It now has " + supports +
            " supporting and " + challenges + " challenging evidence entries.";
    }

    public record CreateBeliefRequest(@NotNull UUID transformationId,
                                      @NotBlank @Size(max = 2000) String statement,
                                      @NotNull BeliefType type) {}

    public record ReviseBeliefRequest(@NotBlank @Size(max = 2000) String statement,
                                      @NotNull BeliefType type,
                                      @NotBlank @Size(max = 1000) String reason,
                                      UUID sourceEvidenceId) {}

    public record CreateEvidenceRequest(@NotBlank @Size(max = 2000) String summary,
                                        @Size(max = 2000) String interpretation,
                                        @NotNull EvidenceDirection direction,
                                        UUID experimentId,
                                        UUID reflectionId,
                                        @NotNull @Valid ProvenanceRequest provenance) {}

    public record ProvenanceRequest(@NotNull ProvenanceSourceKind sourceKind,
                                    @NotNull ProvenanceRecordType recordType,
                                    UUID recordId,
                                    @Size(max = 2000) String excerpt) {}

    public record BeliefDto(UUID id, UUID transformationId, String statement, String type,
                            String createdAt, String revisedAt) {}

    public record BeliefDetailDto(BeliefDto belief, List<BeliefRevisionDto> revisions,
                                  List<EvidenceDto> evidenceTimeline, String narrative) {}

    public record BeliefRevisionDto(UUID id, UUID beliefId, String previousStatement, String newStatement,
                                    String previousType, String newType, String reason,
                                    UUID sourceEvidenceId, String createdAt) {}

    public record EvidenceDto(UUID id, UUID beliefId, UUID experimentId, UUID reflectionId, String summary,
                              String interpretation, String direction, ProvenanceDto provenance,
                              String createdAt) {}

    public record ProvenanceDto(String sourceKind, String recordType, UUID recordId, String excerpt) {}
}