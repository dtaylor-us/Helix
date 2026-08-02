package com.helix.api.shared.application;

import com.helix.api.identity.application.CurrentUserProvider;
import com.helix.api.reflection.application.ReflectionService;
import com.helix.api.shared.adapter.out.persistence.SemanticSearchDocumentRepository;
import com.helix.api.shared.domain.SemanticSearchDocumentEntity;
import com.helix.api.wisdom.application.WisdomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ADR-021: {@code rebuild()} is fully scoped to the calling user -- both source reads
 * ({@code ReflectionService.listForRetrieval()} and {@code WisdomService.list()}) and the index wipe
 * ({@code repository.deleteAllByOwnerId}) only ever touch the caller's own documents.
 */
@Service
public class SemanticIndexingService {

    private final ReflectionService reflectionService;
    private final WisdomService wisdomService;
    private final SemanticSearchDocumentRepository repository;
    private final TextEmbeddingPort textEmbeddingPort;
    private final CurrentUserProvider currentUserProvider;

    public SemanticIndexingService(ReflectionService reflectionService,
                                   WisdomService wisdomService,
                                   SemanticSearchDocumentRepository repository,
                                   TextEmbeddingPort textEmbeddingPort,
                                   CurrentUserProvider currentUserProvider) {
        this.reflectionService = reflectionService;
        this.wisdomService = wisdomService;
        this.repository = repository;
        this.textEmbeddingPort = textEmbeddingPort;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public IndexRebuildResult rebuild() {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var ownerId = currentUserProvider.currentUserId();
        var documents = new ArrayList<SemanticSearchDocumentEntity>();

        reflectionService.listForRetrieval().forEach(reflection -> documents.add(new SemanticSearchDocumentEntity(
            UUID.randomUUID(),
            "REFLECTION",
            reflection.getId(),
            reflection.getContent(),
            serialize(textEmbeddingPort.embed(reflection.getContent())),
            reflection.getCreatedAt(),
            now,
            ownerId
        )));

        wisdomService.list().forEach(wisdom -> documents.add(new SemanticSearchDocumentEntity(
            UUID.randomUUID(),
            "WISDOM",
            wisdom.getId(),
            wisdom.getStatement(),
            serialize(textEmbeddingPort.embed(wisdom.getStatement())),
            wisdom.getRevisedAt(),
            now,
            ownerId
        )));

        repository.deleteAllByOwnerId(ownerId);
        repository.saveAll(documents);

        return new IndexRebuildResult(documents.size(), textEmbeddingPort.modelName(), now.toString());
    }

    public boolean isIndexed() {
        return repository.count() > 0;
    }

    private String serialize(List<Double> embedding) {
        return embedding.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    }

    public record IndexRebuildResult(int indexedCount, String embeddingModel, String indexedAt) {
    }
}