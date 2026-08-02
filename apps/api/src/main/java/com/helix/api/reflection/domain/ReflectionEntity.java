package com.helix.api.reflection.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reflections")
public class ReflectionEntity {

    @Id
    private UUID id;

    @Column(name = "experiment_id", nullable = false)
    private UUID experimentId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column
    private Boolean attempted;

    @Column(columnDefinition = "text")
    private String noticed;

    @Column(name = "evidence_noted", columnDefinition = "text")
    private String evidenceNoted;

    @Column(columnDefinition = "text")
    private String surprise;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // ADR-021: nullable here only so pre-existing test fixtures (never persisted) keep compiling.
    // The database column is NOT NULL -- see TransformationEntity.ownerId for the full rationale.
    @Column(name = "owner_id")
    private UUID ownerId;

    protected ReflectionEntity() {}

    public ReflectionEntity(UUID id, UUID experimentId, String content, OffsetDateTime createdAt) {
        this(id, experimentId, content, null, null, null, null, createdAt);
    }

    public ReflectionEntity(
        UUID id, UUID experimentId, String content, Boolean attempted,
        String noticed, String evidenceNoted, String surprise, OffsetDateTime createdAt
    ) {
        this.id = id;
        this.experimentId = experimentId;
        this.content = content;
        this.attempted = attempted;
        this.noticed = noticed;
        this.evidenceNoted = evidenceNoted;
        this.surprise = surprise;
        this.createdAt = createdAt;
    }

    public ReflectionEntity(
        UUID id, UUID experimentId, String content, Boolean attempted,
        String noticed, String evidenceNoted, String surprise, OffsetDateTime createdAt, UUID ownerId
    ) {
        this(id, experimentId, content, attempted, noticed, evidenceNoted, surprise, createdAt);
        this.ownerId = ownerId;
    }

    public UUID getId() { return id; }
    public UUID getExperimentId() { return experimentId; }
    public String getContent() { return content; }
    public Boolean getAttempted() { return attempted; }
    public String getNoticed() { return noticed; }
    public String getEvidenceNoted() { return evidenceNoted; }
    public String getSurprise() { return surprise; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public UUID getOwnerId() { return ownerId; }
}
