package com.helix.api.memory.application;

import com.helix.api.ai.application.AiAssistantPort;
import com.helix.api.evidence.application.EvidenceService;
import com.helix.api.beliefs.application.BeliefService;
import com.helix.api.experiments.application.ExperimentService;
import com.helix.api.experiments.domain.ExperimentEntity;
import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.memory.adapter.out.persistence.MemoryProposalRepository;
import com.helix.api.memory.adapter.out.persistence.MemoryProposalRevisionRepository;
import com.helix.api.memory.domain.MemoryProposalEntity;
import com.helix.api.memory.domain.MemoryProposalRevisionEntity;
import com.helix.api.memory.domain.MemoryProposalStatus;
import com.helix.api.memory.domain.MemorySourceKind;
import com.helix.api.memory.domain.MemorySourceRecordType;
import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.reflection.domain.ReflectionEntity;
import com.helix.api.wisdom.application.WeeklyRetrospectiveService;
import com.helix.api.wisdom.application.WisdomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class MemoryProposalService {

    private final MemoryProposalRepository repository;
    private final MemoryProposalRevisionRepository revisionRepository;
    private final ExperimentService experimentService;
    private final BeliefService beliefService;
    private final ReflectionService reflectionService;
    private final EvidenceService evidenceService;
    private final WisdomService wisdomService;
    private final WeeklyRetrospectiveService retrospectiveService;
    private final AiAssistantPort aiAssistantPort;
    private final CurrentUserProvider currentUserProvider;

    public MemoryProposalService(MemoryProposalRepository repository,
                                 MemoryProposalRevisionRepository revisionRepository,
                                 ExperimentService experimentService,
                                 BeliefService beliefService,
                                 ReflectionService reflectionService,
                                 EvidenceService evidenceService,
                                 WisdomService wisdomService,
                                 WeeklyRetrospectiveService retrospectiveService,
                                 AiAssistantPort aiAssistantPort,
                                 CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.revisionRepository = revisionRepository;
        this.experimentService = experimentService;
        this.beliefService = beliefService;
        this.reflectionService = reflectionService;
        this.evidenceService = evidenceService;
        this.wisdomService = wisdomService;
        this.retrospectiveService = retrospectiveService;
        this.aiAssistantPort = aiAssistantPort;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public MemoryProposalEntity create(String statement, MemorySourceKind sourceKind,
                                      MemorySourceRecordType sourceRecordType, UUID sourceRecordId,
                                      String sourceExcerpt) {
        validateSource(sourceRecordType, sourceRecordId);
        var now = OffsetDateTime.now();
        return repository.save(new MemoryProposalEntity(
            UUID.randomUUID(),
            statement.trim(),
            MemoryProposalStatus.PROPOSED,
            sourceKind,
            sourceRecordType,
            sourceRecordId,
            sourceExcerpt == null || sourceExcerpt.isBlank() ? null : sourceExcerpt.trim(),
            now,
            now,
            currentUserProvider.currentUserId()
        ));
    }

    public List<MemoryProposalEntity> list() {
        return repository.findAllByOwnerIdOrderByRevisedAtDesc(currentUserProvider.currentUserId());
    }

    /**
     * Propose a candidate memory statement from a just-saved reflection, using AI (ADR-018,
     * Phase 6). Nothing is persisted by this call — per ADR-008, the caller must route the result
     * through the existing {@link #create} flow (which lands it as PROPOSED) before it becomes a
     * real memory. A null {@code statement} on the returned draft means there's nothing worth
     * proposing (either the model judged the reflection didn't reveal a durable pattern, or no AI
     * provider is available) — the caller should simply not show a proposal card in that case,
     * not treat it as an error.
     */
    public MemoryProposalDraft proposeFromReflection(UUID reflectionId) {
        var reflection = reflectionService.get(reflectionId);
        var experiment = experimentService.get(reflection.getExperimentId());
        var aiProposal = aiAssistantPort.proposeMemory(buildMemoryContext(experiment, reflection));
        return new MemoryProposalDraft(
            aiProposal.statement(),
            aiProposal.deterministicFallback() ? "DETERMINISTIC" : "AI",
            aiProposal.provider(),
            aiProposal.model()
        );
    }

    private String buildMemoryContext(ExperimentEntity experiment, ReflectionEntity reflection) {
        var context = new StringBuilder();
        context.append("Experiment: ").append(experiment.getTitle()).append(". ");
        context.append("Reflection: ").append(reflection.getContent()).append(". ");
        if (reflection.getNoticed() != null) {
            context.append("Noticed: ").append(reflection.getNoticed()).append(". ");
        }
        if (reflection.getEvidenceNoted() != null) {
            context.append("Evidence: ").append(reflection.getEvidenceNoted()).append(". ");
        }
        if (reflection.getSurprise() != null) {
            context.append("Surprise: ").append(reflection.getSurprise()).append(". ");
        }
        return context.toString();
    }

    public MemoryProposalEntity get(UUID id) {
        return repository.findByIdAndOwnerId(id, currentUserProvider.currentUserId())
            .orElseThrow(() -> new NoSuchElementException("Memory proposal not found"));
    }

    public List<MemoryProposalRevisionEntity> revisions(UUID id) {
        get(id);
        return revisionRepository.findByMemoryProposalIdOrderByCreatedAtDesc(id);
    }

    @Transactional
    public MemoryProposalRevisionEntity revise(UUID id, String statement, String reason, String sourceExcerpt) {
        var proposal = get(id);
        var now = OffsetDateTime.now();
        var revision = revisionRepository.save(new MemoryProposalRevisionEntity(
            UUID.randomUUID(),
            id,
            proposal.getStatement(),
            statement.trim(),
            proposal.getStatus(),
            MemoryProposalStatus.PROPOSED,
            reason.trim(),
            now,
            currentUserProvider.currentUserId()
        ));
        proposal.revise(statement.trim(), sourceExcerpt == null || sourceExcerpt.isBlank() ? null : sourceExcerpt.trim(), now);
        repository.save(proposal);
        return revision;
    }

    @Transactional
    public MemoryProposalRevisionEntity accept(UUID id, String reason) {
        return review(id, MemoryProposalStatus.CONFIRMED, reason);
    }

    @Transactional
    public MemoryProposalRevisionEntity reject(UUID id, String reason) {
        return review(id, MemoryProposalStatus.REJECTED, reason);
    }

    @Transactional
    public void delete(UUID id) {
        var proposal = get(id);
        repository.delete(proposal);
    }

    private MemoryProposalRevisionEntity review(UUID id, MemoryProposalStatus nextStatus, String reason) {
        var proposal = get(id);
        var now = OffsetDateTime.now();
        var revision = revisionRepository.save(new MemoryProposalRevisionEntity(
            UUID.randomUUID(),
            id,
            proposal.getStatement(),
            proposal.getStatement(),
            proposal.getStatus(),
            nextStatus,
            reason.trim(),
            now,
            currentUserProvider.currentUserId()
        ));
        if (nextStatus == MemoryProposalStatus.CONFIRMED) {
            proposal.accept(now);
        } else {
            proposal.reject(now);
        }
        repository.save(proposal);
        return revision;
    }

    private void validateSource(MemorySourceRecordType sourceRecordType, UUID sourceRecordId) {
        if (sourceRecordId == null) {
            throw new IllegalArgumentException("Source record id is required");
        }

        try {
            switch (sourceRecordType) {
                case REFLECTION -> reflectionService.get(sourceRecordId);
                case EXPERIMENT -> experimentService.get(sourceRecordId);
                case BELIEF -> beliefService.get(sourceRecordId);
                case EVIDENCE -> evidenceService.get(sourceRecordId);
                case WISDOM -> wisdomService.get(sourceRecordId);
                case RETROSPECTIVE -> retrospectiveService.get(sourceRecordId);
                case MANUAL_ENTRY -> {
                }
            }
        } catch (NoSuchElementException ex) {
            throw new IllegalArgumentException("That source record couldn't be found — check the ID/type and try again.");
        }
    }

    public record MemoryProposalDraft(String statement, String source, String aiProvider, String aiModel) {}
}