package com.helix.api.wisdom.application;

import com.helix.api.evidence.application.EvidenceService;
import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.wisdom.adapter.out.persistence.WisdomEntryRepository;
import com.helix.api.wisdom.adapter.out.persistence.WisdomRevisionRepository;
import com.helix.api.wisdom.adapter.out.persistence.WisdomSourceLinkRepository;
import com.helix.api.wisdom.domain.WisdomEntryEntity;
import com.helix.api.wisdom.domain.WisdomRevisionEntity;
import com.helix.api.wisdom.domain.WisdomSourceLinkEntity;
import com.helix.api.wisdom.domain.WisdomSourceType;
import com.helix.api.wisdom.domain.WisdomStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * ADR-021 gap: {@code list}/{@code get}/{@code search} below are NOT YET owner-scoped -- ownerId is
 * set on every write (satisfies the NOT NULL column) but reads still cross every user's wisdom.
 * See the ADR-021 development log entry's gap list before deploying multi-user.
 */
@Service
public class WisdomService {

    private final WisdomEntryRepository repository;
    private final WisdomRevisionRepository revisionRepository;
    private final WisdomSourceLinkRepository sourceLinkRepository;
    private final WeeklyRetrospectiveService retrospectiveService;
    private final ReflectionService reflectionService;
    private final EvidenceService evidenceService;
    private final CurrentUserProvider currentUserProvider;

    public WisdomService(WisdomEntryRepository repository,
                         WisdomRevisionRepository revisionRepository,
                         WisdomSourceLinkRepository sourceLinkRepository,
                         WeeklyRetrospectiveService retrospectiveService,
                         ReflectionService reflectionService,
                         EvidenceService evidenceService,
                         CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.revisionRepository = revisionRepository;
        this.sourceLinkRepository = sourceLinkRepository;
        this.retrospectiveService = retrospectiveService;
        this.reflectionService = reflectionService;
        this.evidenceService = evidenceService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public WisdomEntryEntity create(String statement, UUID retrospectiveId, List<WisdomSourceInput> sources) {
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("At least one source link is required");
        }
        if (retrospectiveId != null) {
            retrospectiveService.get(retrospectiveId);
        }

        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var ownerId = currentUserProvider.currentUserId();
        var entry = repository.save(new WisdomEntryEntity(
            UUID.randomUUID(),
            statement.trim(),
            WisdomStatus.ACCEPTED,
            retrospectiveId,
            now,
            now,
            ownerId
        ));

        for (var source : sources) {
            validateSource(source.sourceType(), source.sourceRecordId());
            sourceLinkRepository.save(new WisdomSourceLinkEntity(
                UUID.randomUUID(),
                entry.getId(),
                source.sourceType(),
                source.sourceRecordId(),
                source.note() == null || source.note().isBlank() ? null : source.note().trim(),
                now,
                ownerId
            ));
        }

        return entry;
    }

    public List<WisdomEntryEntity> list() {
        return repository.findAllByOrderByRevisedAtDesc();
    }

    public WisdomEntryEntity get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Wisdom entry not found"));
    }

    public List<WisdomRevisionEntity> revisionHistory(UUID wisdomId) {
        get(wisdomId);
        return revisionRepository.findByWisdomIdOrderByCreatedAtDesc(wisdomId);
    }

    public List<WisdomSourceLinkEntity> sources(UUID wisdomId) {
        get(wisdomId);
        return sourceLinkRepository.findByWisdomIdOrderByCreatedAtAsc(wisdomId);
    }

    @Transactional
    public WisdomRevisionEntity revise(UUID wisdomId, String statement, String reason) {
        var entry = get(wisdomId);
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var revision = revisionRepository.save(new WisdomRevisionEntity(
            UUID.randomUUID(),
            wisdomId,
            entry.getStatement(),
            statement.trim(),
            reason.trim(),
            now,
            currentUserProvider.currentUserId()
        ));
        entry.revise(statement.trim(), now);
        repository.save(entry);
        return revision;
    }

    public List<WisdomEntryEntity> search(String query) {
        return repository.findTop20ByStatementContainingIgnoreCaseOrderByRevisedAtDesc(query.trim());
    }

    private void validateSource(WisdomSourceType sourceType, UUID sourceRecordId) {
        if (sourceRecordId == null) {
            throw new IllegalArgumentException("Source record id is required");
        }

        switch (sourceType) {
            case REFLECTION -> reflectionService.get(sourceRecordId);
            case EVIDENCE -> evidenceService.get(sourceRecordId);
            case RETROSPECTIVE -> retrospectiveService.get(sourceRecordId);
        }
    }

    public record WisdomSourceInput(WisdomSourceType sourceType, UUID sourceRecordId, String note) {}
}
