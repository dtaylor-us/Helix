package com.helix.api.transformation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transformations")
public class TransformationEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(columnDefinition = "text")
    private String purpose;

    @Column(name = "desired_identity", columnDefinition = "text")
    private String desiredIdentity;

    @Column(columnDefinition = "text")
    private String obstacle;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // ADR-021: nullable in this class only so the pre-existing shorter constructors below (used
    // throughout the existing test suite as in-memory fixtures that are never persisted) keep
    // compiling unchanged. The database column itself is NOT NULL (V12 migration) -- every
    // production code path must go through the ownerId-taking constructor, or the real insert
    // fails loudly rather than silently leaving a row unscoped.
    @Column(name = "owner_id")
    private UUID ownerId;

    protected TransformationEntity() {}

    public TransformationEntity(UUID id, String title, String purpose, OffsetDateTime createdAt) {
        this(id, title, purpose, null, null, createdAt);
    }

    public TransformationEntity(
        UUID id, String title, String purpose, String desiredIdentity, String obstacle, OffsetDateTime createdAt
    ) {
        this.id = id;
        this.title = title;
        this.purpose = purpose;
        this.desiredIdentity = desiredIdentity;
        this.obstacle = obstacle;
        this.createdAt = createdAt;
    }

    public TransformationEntity(
        UUID id, String title, String purpose, String desiredIdentity, String obstacle,
        OffsetDateTime createdAt, UUID ownerId
    ) {
        this(id, title, purpose, desiredIdentity, obstacle, createdAt);
        this.ownerId = ownerId;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getPurpose() { return purpose; }
    public String getDesiredIdentity() { return desiredIdentity; }
    public String getObstacle() { return obstacle; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public UUID getOwnerId() { return ownerId; }
}
