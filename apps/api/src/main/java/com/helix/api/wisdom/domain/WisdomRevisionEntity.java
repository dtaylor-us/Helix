package com.helix.api.wisdom.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "wisdom_revisions")
public class WisdomRevisionEntity {

    @Id
    private UUID id;

    @Column(name = "wisdom_id", nullable = false)
    private UUID wisdomId;

    @Column(name = "previous_statement", nullable = false, columnDefinition = "text")
    private String previousStatement;

    @Column(name = "new_statement", nullable = false, columnDefinition = "text")
    private String newStatement;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // ADR-021: nullable here only so pre-existing test fixtures (never persisted) keep compiling.
    @Column(name = "owner_id")
    private UUID ownerId;

    protected WisdomRevisionEntity() {}

    public WisdomRevisionEntity(UUID id, UUID wisdomId, String previousStatement, String newStatement,
                                String reason, OffsetDateTime createdAt) {
        this.id = id;
        this.wisdomId = wisdomId;
        this.previousStatement = previousStatement;
        this.newStatement = newStatement;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public WisdomRevisionEntity(UUID id, UUID wisdomId, String previousStatement, String newStatement,
                                String reason, OffsetDateTime createdAt, UUID ownerId) {
        this(id, wisdomId, previousStatement, newStatement, reason, createdAt);
        this.ownerId = ownerId;
    }

    public UUID getId() { return id; }
    public UUID getWisdomId() { return wisdomId; }
    public String getPreviousStatement() { return previousStatement; }
    public String getNewStatement() { return newStatement; }
    public String getReason() { return reason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public UUID getOwnerId() { return ownerId; }
}
