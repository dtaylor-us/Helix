package com.helix.api.data.adapter.in.http;

import com.helix.api.beliefs.domain.BeliefEntity;
import com.helix.api.beliefs.domain.BeliefRevisionEntity;
import com.helix.api.data.application.DataDeletionService;
import com.helix.api.data.application.DataExportService;
import com.helix.api.evidence.domain.EvidenceEntity;
import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.memory.domain.MemoryProposalEntity;
import com.helix.api.memory.domain.MemoryProposalRevisionEntity;
import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.suggestions.domain.SuggestionEntity;
import com.helix.api.transformation.domain.TransformationEntity;
import com.helix.api.wisdom.domain.WeeklyRetrospectiveEntity;
import com.helix.api.wisdom.domain.WisdomEntryEntity;
import com.helix.api.wisdom.domain.WisdomRevisionEntity;
import com.helix.api.wisdom.domain.WisdomSourceLinkEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Phase 9 (ADR-015, ADR-019): data export and whole-app deletion. Single-user today (ADR-013 defers
 * auth), so there is no per-user scoping on either endpoint.
 */
@RestController
@RequestMapping("/api/v1/data")
public class DataController {

    private final DataExportService exportService;
    private final DataDeletionService deletionService;

    public DataController(DataExportService exportService, DataDeletionService deletionService) {
        this.exportService = exportService;
        this.deletionService = deletionService;
    }

    @GetMapping("/export")
    public DataExportDto export() {
        var snapshot = exportService.export();
        return new DataExportDto(
            snapshot.onboardingStatus().name(),
            snapshot.transformations().stream().map(this::toTransformation).toList(),
            snapshot.experiments().stream().map(this::toExperiment).toList(),
            snapshot.reflections().stream().map(this::toReflection).toList(),
            snapshot.suggestions().stream().map(this::toSuggestion).toList(),
            snapshot.beliefs().stream().map(this::toBelief).toList(),
            snapshot.beliefRevisions().stream().map(this::toBeliefRevision).toList(),
            snapshot.evidence().stream().map(this::toEvidence).toList(),
            snapshot.weeklyRetrospectives().stream().map(this::toRetrospective).toList(),
            snapshot.wisdomEntries().stream().map(this::toWisdomEntry).toList(),
            snapshot.wisdomRevisions().stream().map(this::toWisdomRevision).toList(),
            snapshot.wisdomSourceLinks().stream().map(this::toWisdomSourceLink).toList(),
            snapshot.memoryProposals().stream().map(this::toMemoryProposal).toList(),
            snapshot.memoryProposalRevisions().stream().map(this::toMemoryProposalRevision).toList()
        );
    }

    /**
     * Irreversible whole-app hard delete (ADR-019). Requires an explicit {@code confirm: true} in
     * the body — not a real security control (the caller fully controls the body), but it means a
     * bare, no-body DELETE can't destroy everything by accident.
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAll(@Valid @RequestBody DeleteAllDataRequest request) {
        if (!request.confirm()) {
            throw new IllegalArgumentException("Deletion requires confirm: true in the request body.");
        }
        deletionService.deleteEverything();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private TransformationDto toTransformation(TransformationEntity e) {
        return new TransformationDto(e.getId(), e.getTitle(), e.getPurpose(), e.getDesiredIdentity(),
            e.getObstacle(), e.getCreatedAt().toString());
    }

    private ExperimentDto toExperiment(ExperimentEntity e) {
        return new ExperimentDto(e.getId(), e.getTransformationId(), e.getTitle(), e.getHypothesis(),
            e.getNextAction(), e.getCadence(), e.getEvidenceOfSuccess(),
            e.getReviewAt() != null ? e.getReviewAt().toString() : null, e.getStatus().name(),
            e.getCreatedAt().toString());
    }

    private ReflectionDto toReflection(ReflectionEntity e) {
        return new ReflectionDto(e.getId(), e.getExperimentId(), e.getContent(), e.getAttempted(),
            e.getNoticed(), e.getEvidenceNoted(), e.getSurprise(), e.getCreatedAt().toString());
    }

    private SuggestionDto toSuggestion(SuggestionEntity e) {
        return new SuggestionDto(e.getId(), e.getExperimentId(), e.getReflectionId(), e.getText(),
            e.getStatus().name(), e.getReplacementText(), e.getCreatedAt().toString(),
            e.getRespondedAt() != null ? e.getRespondedAt().toString() : null,
            e.getSource().name(), e.getAiProvider(), e.getAiModel());
    }

    private BeliefDto toBelief(BeliefEntity e) {
        return new BeliefDto(e.getId(), e.getTransformationId(), e.getStatement(), e.getType().name(),
            e.getCreatedAt().toString(), e.getRevisedAt().toString());
    }

    private BeliefRevisionDto toBeliefRevision(BeliefRevisionEntity e) {
        return new BeliefRevisionDto(e.getId(), e.getBeliefId(), e.getPreviousStatement(), e.getNewStatement(),
            e.getPreviousType().name(), e.getNewType().name(), e.getReason(), e.getSourceEvidenceId(),
            e.getCreatedAt().toString());
    }

    private EvidenceDto toEvidence(EvidenceEntity e) {
        return new EvidenceDto(e.getId(), e.getBeliefId(), e.getExperimentId(), e.getReflectionId(),
            e.getSummary(), e.getInterpretation(), e.getDirection().name(),
            e.getProvenanceSourceKind().name(), e.getProvenanceRecordType().name(),
            e.getProvenanceRecordId(), e.getProvenanceExcerpt(), e.getCreatedAt().toString());
    }

    private RetrospectiveDto toRetrospective(WeeklyRetrospectiveEntity e) {
        return new RetrospectiveDto(e.getId(), e.getPeriodStart().toString(), e.getPeriodEnd().toString(),
            e.getSummary(), e.getAssistance(), e.getCreatedAt().toString(), e.getSource().name(),
            e.getAiProvider(), e.getAiModel());
    }

    private WisdomEntryDto toWisdomEntry(WisdomEntryEntity e) {
        return new WisdomEntryDto(e.getId(), e.getStatement(), e.getStatus().name(), e.getRetrospectiveId(),
            e.getCreatedAt().toString(), e.getRevisedAt().toString());
    }

    private WisdomRevisionDto toWisdomRevision(WisdomRevisionEntity e) {
        return new WisdomRevisionDto(e.getId(), e.getWisdomId(), e.getPreviousStatement(), e.getNewStatement(),
            e.getReason(), e.getCreatedAt().toString());
    }

    private WisdomSourceLinkDto toWisdomSourceLink(WisdomSourceLinkEntity e) {
        return new WisdomSourceLinkDto(e.getId(), e.getWisdomId(), e.getSourceType().name(),
            e.getSourceRecordId(), e.getNote(), e.getCreatedAt().toString());
    }

    private MemoryProposalDto toMemoryProposal(MemoryProposalEntity e) {
        return new MemoryProposalDto(e.getId(), e.getStatement(), e.getStatus().name(), e.getSourceKind().name(),
            e.getSourceRecordType().name(), e.getSourceRecordId(), e.getSourceExcerpt(),
            e.getCreatedAt().toString(), e.getRevisedAt().toString());
    }

    private MemoryProposalRevisionDto toMemoryProposalRevision(MemoryProposalRevisionEntity e) {
        return new MemoryProposalRevisionDto(e.getId(), e.getMemoryProposalId(), e.getPreviousStatement(),
            e.getNewStatement(), e.getPreviousStatus().name(), e.getNewStatus().name(), e.getReason(),
            e.getCreatedAt().toString());
    }

    public record DeleteAllDataRequest(boolean confirm) {}

    public record DataExportDto(
        String onboardingStatus,
        java.util.List<TransformationDto> transformations,
        java.util.List<ExperimentDto> experiments,
        java.util.List<ReflectionDto> reflections,
        java.util.List<SuggestionDto> suggestions,
        java.util.List<BeliefDto> beliefs,
        java.util.List<BeliefRevisionDto> beliefRevisions,
        java.util.List<EvidenceDto> evidence,
        java.util.List<RetrospectiveDto> weeklyRetrospectives,
        java.util.List<WisdomEntryDto> wisdomEntries,
        java.util.List<WisdomRevisionDto> wisdomRevisions,
        java.util.List<WisdomSourceLinkDto> wisdomSourceLinks,
        java.util.List<MemoryProposalDto> memoryProposals,
        java.util.List<MemoryProposalRevisionDto> memoryProposalRevisions
    ) {}

    public record TransformationDto(UUID id, String title, String purpose, String desiredIdentity,
                                    String obstacle, String createdAt) {}
    public record ExperimentDto(UUID id, UUID transformationId, String title, String hypothesis,
                                String nextAction, String cadence, String evidenceOfSuccess, String reviewAt,
                                String status, String createdAt) {}
    public record ReflectionDto(UUID id, UUID experimentId, String content, Boolean attempted,
                                String noticed, String evidenceNoted, String surprise, String createdAt) {}
    public record SuggestionDto(UUID id, UUID experimentId, UUID reflectionId, String text,
                                String status, String replacementText, String createdAt, String respondedAt,
                                String source, String aiProvider, String aiModel) {}
    public record BeliefDto(UUID id, UUID transformationId, String statement, String type,
                            String createdAt, String revisedAt) {}
    public record BeliefRevisionDto(UUID id, UUID beliefId, String previousStatement, String newStatement,
                                    String previousType, String newType, String reason, UUID sourceEvidenceId,
                                    String createdAt) {}
    public record EvidenceDto(UUID id, UUID beliefId, UUID experimentId, UUID reflectionId, String summary,
                              String interpretation, String direction, String provenanceSourceKind,
                              String provenanceRecordType, UUID provenanceRecordId, String provenanceExcerpt,
                              String createdAt) {}
    public record RetrospectiveDto(UUID id, String periodStart, String periodEnd, String summary,
                                   String assistance, String createdAt, String source, String aiProvider,
                                   String aiModel) {}
    public record WisdomEntryDto(UUID id, String statement, String status, UUID retrospectiveId,
                                 String createdAt, String revisedAt) {}
    public record WisdomRevisionDto(UUID id, UUID wisdomId, String previousStatement, String newStatement,
                                    String reason, String createdAt) {}
    public record WisdomSourceLinkDto(UUID id, UUID wisdomId, String sourceType, UUID sourceRecordId,
                                      String note, String createdAt) {}
    public record MemoryProposalDto(UUID id, String statement, String status, String sourceKind,
                                    String sourceRecordType, UUID sourceRecordId, String sourceExcerpt,
                                    String createdAt, String revisedAt) {}
    public record MemoryProposalRevisionDto(UUID id, UUID memoryProposalId, String previousStatement,
                                            String newStatement, String previousStatus, String newStatus,
                                            String reason, String createdAt) {}
}
