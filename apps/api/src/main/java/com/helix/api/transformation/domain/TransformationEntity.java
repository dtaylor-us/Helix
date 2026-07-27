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

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected TransformationEntity() {}

    public TransformationEntity(UUID id, String title, String purpose, OffsetDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.purpose = purpose;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getPurpose() { return purpose; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
