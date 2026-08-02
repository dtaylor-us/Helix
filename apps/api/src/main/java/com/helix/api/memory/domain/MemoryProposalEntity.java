package com.helix.api.memory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "memory_proposals")
public class MemoryProposalEntity {

    @Id
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String statement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MemoryProposalStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_kind", nullable = false, length = 32)
    private MemorySourceKind sourceKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_record_type", nullable = false, length = 32)
    private MemorySourceRecordType sourceRecordType;

    @Column(name = "source_record_id", nullable = false)
    private UUID sourceRecordId;

    @Column(name = "source_excerpt", columnDefinition = "text")
    private String sourceExcerpt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "revised_at", nullable = false)
    private OffsetDateTime revisedAt;

    // ADR-021: nullable here only so pre-existing test fixtures (never persisted) keep compiling.
    // Set on write, but read paths (list/get) are NOT YET owner-scoped -- see MemoryProposalService.
    @Column(name = "owner_id")
    private UUID ownerId;

    protected MemoryProposalEntity() {
    }

    public MemoryProposalEntity(UUID id, String statement, MemoryProposalStatus status, MemorySourceKind sourceKind,
                                MemorySourceRecordType sourceRecordType, UUID sourceRecordId, String sourceExcerpt,
                                OffsetDateTime createdAt, OffsetDateTime revisedAt) {
        this.id = id;
        this.statement = statement;
        this.status = status;
        this.sourceKind = sourceKind;
        this.sourceRecordType = sourceRecordType;
        this.sourceRecordId = sourceRecordId;
        this.sourceExcerpt = sourceExcerpt;
        this.createdAt = createdAt;
        this.revisedAt = revisedAt;
    }

    public MemoryProposalEntity(UUID id, String statement, MemoryProposalStatus status, MemorySourceKind sourceKind,
                                MemorySourceRecordType sourceRecordType, UUID sourceRecordId, String sourceExcerpt,
                                OffsetDateTime createdAt, OffsetDateTime revisedAt, UUID ownerId) {
        this(id, statement, status, sourceKind, sourceRecordType, sourceRecordId, sourceExcerpt, createdAt, revisedAt);
        this.ownerId = ownerId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getId() {
        return id;
    }

    public String getStatement() {
        return statement;
    }

    public MemoryProposalStatus getStatus() {
        return status;
    }

    public MemorySourceKind getSourceKind() {
        return sourceKind;
    }

    public MemorySourceRecordType getSourceRecordType() {
        return sourceRecordType;
    }

    public UUID getSourceRecordId() {
        return sourceRecordId;
    }

    public String getSourceExcerpt() {
        return sourceExcerpt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getRevisedAt() {
        return revisedAt;
    }

    public void revise(String statement, String sourceExcerpt, OffsetDateTime revisedAt) {
        this.statement = statement;
        this.sourceExcerpt = sourceExcerpt;
        this.status = MemoryProposalStatus.PROPOSED;
        this.revisedAt = revisedAt;
    }

    public void accept(OffsetDateTime revisedAt) {
        this.status = MemoryProposalStatus.CONFIRMED;
        this.revisedAt = revisedAt;
    }

    public void reject(OffsetDateTime revisedAt) {
        this.status = MemoryProposalStatus.REJECTED;
        this.revisedAt = revisedAt;
    }
}