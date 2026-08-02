package com.helix.api.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "semantic_search_documents", uniqueConstraints = {
    @UniqueConstraint(name = "uq_semantic_search_record", columnNames = {"record_type", "record_id"})
})
public class SemanticSearchDocumentEntity {

    @Id
    private UUID id;

    @Column(name = "record_type", nullable = false, length = 32)
    private String recordType;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Column(nullable = false, columnDefinition = "text")
    private String snippet;

    @Column(name = "embedding_values", nullable = false, columnDefinition = "text")
    private String embeddingValues;

    @Column(name = "source_updated_at", nullable = false)
    private OffsetDateTime sourceUpdatedAt;

    @Column(name = "indexed_at", nullable = false)
    private OffsetDateTime indexedAt;

    // ADR-021: nullable here only so pre-existing test fixtures (never persisted) keep compiling.
    // ADR-021: set on write; SemanticIndexingService.rebuild() scopes both the source reads and the
    // index wipe (repository.deleteAllByOwnerId) to the caller's own documents.
    @Column(name = "owner_id")
    private UUID ownerId;

    protected SemanticSearchDocumentEntity() {
    }

    public SemanticSearchDocumentEntity(UUID id,
                                        String recordType,
                                        UUID recordId,
                                        String snippet,
                                        String embeddingValues,
                                        OffsetDateTime sourceUpdatedAt,
                                        OffsetDateTime indexedAt) {
        this.id = id;
        this.recordType = recordType;
        this.recordId = recordId;
        this.snippet = snippet;
        this.embeddingValues = embeddingValues;
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.indexedAt = indexedAt;
    }

    public SemanticSearchDocumentEntity(UUID id,
                                        String recordType,
                                        UUID recordId,
                                        String snippet,
                                        String embeddingValues,
                                        OffsetDateTime sourceUpdatedAt,
                                        OffsetDateTime indexedAt,
                                        UUID ownerId) {
        this(id, recordType, recordId, snippet, embeddingValues, sourceUpdatedAt, indexedAt);
        this.ownerId = ownerId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getId() {
        return id;
    }

    public String getRecordType() {
        return recordType;
    }

    public UUID getRecordId() {
        return recordId;
    }

    public String getSnippet() {
        return snippet;
    }

    public String getEmbeddingValues() {
        return embeddingValues;
    }

    public OffsetDateTime getSourceUpdatedAt() {
        return sourceUpdatedAt;
    }

    public OffsetDateTime getIndexedAt() {
        return indexedAt;
    }
}