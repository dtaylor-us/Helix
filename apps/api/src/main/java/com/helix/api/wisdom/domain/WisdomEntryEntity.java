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
@Table(name = "wisdom_entries")
public class WisdomEntryEntity {

    @Id
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String statement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WisdomStatus status;

    @Column(name = "retrospective_id")
    private UUID retrospectiveId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "revised_at", nullable = false)
    private OffsetDateTime revisedAt;

    protected WisdomEntryEntity() {}

    public WisdomEntryEntity(UUID id, String statement, WisdomStatus status, UUID retrospectiveId,
                             OffsetDateTime createdAt, OffsetDateTime revisedAt) {
        this.id = id;
        this.statement = statement;
        this.status = status;
        this.retrospectiveId = retrospectiveId;
        this.createdAt = createdAt;
        this.revisedAt = revisedAt;
    }

    public UUID getId() { return id; }
    public String getStatement() { return statement; }
    public WisdomStatus getStatus() { return status; }
    public UUID getRetrospectiveId() { return retrospectiveId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getRevisedAt() { return revisedAt; }

    public void revise(String newStatement, OffsetDateTime revisedAt) {
        this.statement = newStatement;
        this.revisedAt = revisedAt;
    }
}
