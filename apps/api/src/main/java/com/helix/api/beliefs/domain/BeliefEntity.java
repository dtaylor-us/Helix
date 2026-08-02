package com.helix.api.beliefs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "beliefs")
public class BeliefEntity {

    @Id
    private UUID id;

    @Column(name = "transformation_id", nullable = false)
    private UUID transformationId;

    @Column(nullable = false, columnDefinition = "text")
    private String statement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BeliefType type;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "revised_at", nullable = false)
    private OffsetDateTime revisedAt;

    // ADR-021: nullable here only so pre-existing test fixtures (never persisted) keep compiling.
    // The database column is NOT NULL -- see TransformationEntity.ownerId for the full rationale.
    @Column(name = "owner_id")
    private UUID ownerId;

    protected BeliefEntity() {}

    public BeliefEntity(UUID id, UUID transformationId, String statement, BeliefType type,
                        OffsetDateTime createdAt, OffsetDateTime revisedAt) {
        this.id = id;
        this.transformationId = transformationId;
        this.statement = statement;
        this.type = type;
        this.createdAt = createdAt;
        this.revisedAt = revisedAt;
    }

    public BeliefEntity(UUID id, UUID transformationId, String statement, BeliefType type,
                        OffsetDateTime createdAt, OffsetDateTime revisedAt, UUID ownerId) {
        this(id, transformationId, statement, type, createdAt, revisedAt);
        this.ownerId = ownerId;
    }

    public UUID getId() { return id; }
    public UUID getTransformationId() { return transformationId; }
    public String getStatement() { return statement; }
    public BeliefType getType() { return type; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getRevisedAt() { return revisedAt; }
    public UUID getOwnerId() { return ownerId; }

    public void revise(String statement, BeliefType type, OffsetDateTime revisedAt) {
        this.statement = statement;
        this.type = type;
        this.revisedAt = revisedAt;
    }
}