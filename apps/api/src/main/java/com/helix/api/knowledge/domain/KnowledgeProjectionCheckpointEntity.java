package com.helix.api.knowledge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * First-release freshness tracking only -- one row per source module recording when it was last
 * folded into a full rebuild. Not used for incremental sync (see
 * docs/product/knowledge-graph-scoping.md Section 12, Q16).
 */
@Entity
@Table(name = "knowledge_projection_checkpoint")
public class KnowledgeProjectionCheckpointEntity {

    @Id
    private UUID id;

    @Column(name = "source_module", nullable = false, length = 64)
    private String sourceModule;

    @Column(name = "last_projected_at", nullable = false)
    private OffsetDateTime lastProjectedAt;

    // ADR-021 gap: NOT YET set by KnowledgeGraphProjectionService -- see KnowledgeNodeEntity.ownerId.
    @Column(name = "owner_id")
    private UUID ownerId;

    protected KnowledgeProjectionCheckpointEntity() {}

    public KnowledgeProjectionCheckpointEntity(UUID id, String sourceModule, OffsetDateTime lastProjectedAt) {
        this.id = id;
        this.sourceModule = sourceModule;
        this.lastProjectedAt = lastProjectedAt;
    }

    public KnowledgeProjectionCheckpointEntity(UUID id, String sourceModule, OffsetDateTime lastProjectedAt, UUID ownerId) {
        this(id, sourceModule, lastProjectedAt);
        this.ownerId = ownerId;
    }

    public UUID getOwnerId() { return ownerId; }
    public UUID getId() { return id; }
    public String getSourceModule() { return sourceModule; }
    public OffsetDateTime getLastProjectedAt() { return lastProjectedAt; }

    public void touch(OffsetDateTime now) {
        this.lastProjectedAt = now;
    }
}
