package com.helix.api.beliefs.application;

import com.helix.api.beliefs.adapter.out.persistence.BeliefRepository;
import com.helix.api.beliefs.adapter.out.persistence.BeliefRevisionRepository;
import com.helix.api.beliefs.domain.BeliefEntity;
import com.helix.api.beliefs.domain.BeliefRevisionEntity;
import com.helix.api.beliefs.domain.BeliefType;
import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.transformation.application.TransformationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class BeliefService {

    private final BeliefRepository repository;
    private final BeliefRevisionRepository revisionRepository;
    private final TransformationService transformationService;
    private final CurrentUserProvider currentUserProvider;

    public BeliefService(BeliefRepository repository, BeliefRevisionRepository revisionRepository,
                         TransformationService transformationService, CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.revisionRepository = revisionRepository;
        this.transformationService = transformationService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public BeliefEntity create(UUID transformationId, String statement, BeliefType type) {
        // transformationService.get() 404s if transformationId doesn't belong to the caller.
        transformationService.get(transformationId);
        var now = OffsetDateTime.now();
        var belief = new BeliefEntity(
            UUID.randomUUID(), transformationId, statement.trim(), type, now, now, currentUserProvider.currentUserId()
        );
        return repository.save(belief);
    }

    public List<BeliefEntity> list() {
        return repository.findAllByOwnerIdOrderByRevisedAtDesc(currentUserProvider.currentUserId());
    }

    public BeliefEntity get(UUID id) {
        return repository.findByIdAndOwnerId(id, currentUserProvider.currentUserId())
            .orElseThrow(() -> new NoSuchElementException("Belief not found"));
    }

    public List<BeliefEntity> search(String query) {
        return repository.findTop20ByOwnerIdAndStatementContainingIgnoreCaseOrderByRevisedAtDesc(
            currentUserProvider.currentUserId(), query.trim());
    }

    public List<BeliefRevisionEntity> revisionHistory(UUID beliefId) {
        get(beliefId);
        return revisionRepository.findByBeliefIdOrderByCreatedAtDesc(beliefId);
    }

    @Transactional
    public BeliefRevisionEntity revise(UUID beliefId, String statement, BeliefType type, String reason, UUID sourceEvidenceId) {
        var belief = get(beliefId);
        var revision = new BeliefRevisionEntity(
            UUID.randomUUID(),
            beliefId,
            belief.getStatement(),
            statement.trim(),
            belief.getType(),
            type,
            reason.trim(),
            sourceEvidenceId,
            OffsetDateTime.now(),
            currentUserProvider.currentUserId()
        );
        belief.revise(statement.trim(), type, revision.getCreatedAt());
        revisionRepository.save(revision);
        return revision;
    }
}