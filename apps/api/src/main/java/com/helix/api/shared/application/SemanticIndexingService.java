package com.helix.api.shared.application;

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

@Service
public class SemanticIndexingService {

    private final ReflectionService reflectionService;
    private final WisdomService wisdomService;
    private final SemanticSearchDocumentRepository repository;
    private final TextEmbeddingPort textEmbeddingPort;

    public SemanticIndexingService(ReflectionService reflectionService,
                                   WisdomService wisdomService,
                                   SemanticSearchDocumentRepository repository,
                                   TextEmbeddingPort textEmbeddingPort) {
        this.reflectionService = reflectionService;
        this.wisdomService = wisdomService;
        this.repository = repository;
        this.textEmbeddingPort = textEmbeddingPort;
    }

    @Transactional
    public IndexRebuildResult rebuild() {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var documents = new ArrayList<SemanticSearchDocumentEntity>();

        reflectionService.listForRetrieval().forEach(reflection -> documents.add(new SemanticSearchDocumentEntity(
            UUID.randomUUID(),
            "REFLECTION",
            reflection.getId(),
            reflection.getContent(),
            serialize(textEmbeddingPort.embed(reflection.getContent())),
            reflection.getCreatedAt(),
            now
        )));

        wisdomService.list().forEach(wisdom -> documents.add(new SemanticSearchDocumentEntity(
            UUID.randomUUID(),
            "WISDOM",
            wisdom.getId(),
            wisdom.getStatement(),
            serialize(textEmbeddingPort.embed(wisdom.getStatement())),
            wisdom.getRevisedAt(),
            now
        )));

        repository.deleteAllInBatch();
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