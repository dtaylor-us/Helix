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
@Table(name = "belief_revisions")
public class BeliefRevisionEntity {

    @Id
    private UUID id;

    @Column(name = "belief_id", nullable = false)
    private UUID beliefId;

    @Column(name = "previous_statement", nullable = false, columnDefinition = "text")
    private String previousStatement;

    @Column(name = "new_statement", nullable = false, columnDefinition = "text")
    private String newStatement;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_type", nullable = false, length = 32)
    private BeliefType previousType;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_type", nullable = false, length = 32)
    private BeliefType newType;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "source_evidence_id")
    private UUID sourceEvidenceId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // ADR-021: nullable here only so pre-existing test fixtures (never persisted) keep compiling.
    // The database column is NOT NULL -- see TransformationEntity.ownerId for the full rationale.
    @Column(name = "owner_id")
    private UUID ownerId;

    protected BeliefRevisionEntity() {}

    public BeliefRevisionEntity(UUID id, UUID beliefId, String previousStatement, String newStatement,
                                BeliefType previousType, BeliefType newType, String reason,
                                UUID sourceEvidenceId, OffsetDateTime createdAt) {
        this.id = id;
        this.beliefId = beliefId;
        this.previousStatement = previousStatement;
        this.newStatement = newStatement;
        this.previousType = previousType;
        this.newType = newType;
        this.reason = reason;
        this.sourceEvidenceId = sourceEvidenceId;
        this.createdAt = createdAt;
    }

    public BeliefRevisionEntity(UUID id, UUID beliefId, String previousStatement, String newStatement,
                                BeliefType previousType, BeliefType newType, String reason,
                                UUID sourceEvidenceId, OffsetDateTime createdAt, UUID ownerId) {
        this(id, beliefId, previousStatement, newStatement, previousType, newType, reason, sourceEvidenceId, createdAt);
        this.ownerId = ownerId;
    }

    public UUID getId() { return id; }
    public UUID getBeliefId() { return beliefId; }
    public String getPreviousStatement() { return previousStatement; }
    public String getNewStatement() { return newStatement; }
    public BeliefType getPreviousType() { return previousType; }
    public BeliefType getNewType() { return newType; }
    public String getReason() { return reason; }
    public UUID getSourceEvidenceId() { return sourceEvidenceId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public UUID getOwnerId() { return ownerId; }
}