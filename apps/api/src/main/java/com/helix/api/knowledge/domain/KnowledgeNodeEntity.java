package com.helix.api.knowledge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "knowledge_node")
public class KnowledgeNodeEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 32)
    private KnowledgeNodeType nodeType;

    @Column(name = "source_record_id", nullable = false)
    private UUID sourceRecordId;

    @Column(name = "display_label", nullable = false, columnDefinition = "text")
    private String displayLabel;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "lifecycle_status", length = 32)
    private String lifecycleStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ADR-021: set by KnowledgeGraphProjectionService on every rebuild.
    @Column(name = "owner_id")
    private UUID ownerId;

    protected KnowledgeNodeEntity() {}

    public KnowledgeNodeEntity(
        UUID id, KnowledgeNodeType nodeType, UUID sourceRecordId, String displayLabel, String summary,
        String lifecycleStatus, OffsetDateTime createdAt, OffsetDateTime updatedAt
    ) {
        this.id = id;
        this.nodeType = nodeType;
        this.sourceRecordId = sourceRecordId;
        this.displayLabel = displayLabel;
        this.summary = summary;
        this.lifecycleStatus = lifecycleStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public KnowledgeNodeEntity(
        UUID id, KnowledgeNodeType nodeType, UUID sourceRecordId, String displayLabel, String summary,
        String lifecycleStatus, OffsetDateTime createdAt, OffsetDateTime updatedAt, UUID ownerId
    ) {
        this(id, nodeType, sourceRecordId, displayLabel, summary, lifecycleStatus, createdAt, updatedAt);
        this.ownerId = ownerId;
    }

    public UUID getOwnerId() { return ownerId; }
    public UUID getId() { return id; }
    public KnowledgeNodeType getNodeType() { return nodeType; }
    public UUID getSourceRecordId() { return sourceRecordId; }
    public String getDisplayLabel() { return displayLabel; }
    public String getSummary() { return summary; }
    public String getLifecycleStatus() { return lifecycleStatus; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
