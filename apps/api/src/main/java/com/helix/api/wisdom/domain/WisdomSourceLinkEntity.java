package com.helix.api.wisdom.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "wisdom_source_links")
public class WisdomSourceLinkEntity {

    @Id
    private UUID id;

    @Column(name = "wisdom_id", nullable = false)
    private UUID wisdomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private WisdomSourceType sourceType;

    @Column(name = "source_record_id", nullable = false)
    private UUID sourceRecordId;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // ADR-021: nullable here only so pre-existing test fixtures (never persisted) keep compiling.
    @Column(name = "owner_id")
    private UUID ownerId;

    protected WisdomSourceLinkEntity() {}

    public WisdomSourceLinkEntity(UUID id, UUID wisdomId, WisdomSourceType sourceType, UUID sourceRecordId,
                                  String note, OffsetDateTime createdAt) {
        this.id = id;
        this.wisdomId = wisdomId;
        this.sourceType = sourceType;
        this.sourceRecordId = sourceRecordId;
        this.note = note;
        this.createdAt = createdAt;
    }

    public WisdomSourceLinkEntity(UUID id, UUID wisdomId, WisdomSourceType sourceType, UUID sourceRecordId,
                                  String note, OffsetDateTime createdAt, UUID ownerId) {
        this(id, wisdomId, sourceType, sourceRecordId, note, createdAt);
        this.ownerId = ownerId;
    }

    public UUID getId() { return id; }
    public UUID getWisdomId() { return wisdomId; }
    public WisdomSourceType getSourceType() { return sourceType; }
    public UUID getSourceRecordId() { return sourceRecordId; }
    public String getNote() { return note; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public UUID getOwnerId() { return ownerId; }
}
