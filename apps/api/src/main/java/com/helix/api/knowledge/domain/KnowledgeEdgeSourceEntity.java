package com.helix.api.knowledge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** A single authoritative record that supports/justifies a {@link KnowledgeEdgeEntity}. */
@Entity
@Table(name = "knowledge_edge_source")
public class KnowledgeEdgeSourceEntity {

    @Id
    private UUID id;

    @Column(name = "knowledge_edge_id", nullable = false)
    private UUID knowledgeEdgeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false, length = 32)
    private KnowledgeNodeType recordType;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    // ADR-021: set by KnowledgeGraphProjectionService and KnowledgeGraphRelationshipDiscoveryService.
    @Column(name = "owner_id")
    private UUID ownerId;

    protected KnowledgeEdgeSourceEntity() {}

    public KnowledgeEdgeSourceEntity(UUID id, UUID knowledgeEdgeId, KnowledgeNodeType recordType, UUID recordId) {
        this.id = id;
        this.knowledgeEdgeId = knowledgeEdgeId;
        this.recordType = recordType;
        this.recordId = recordId;
    }

    public KnowledgeEdgeSourceEntity(UUID id, UUID knowledgeEdgeId, KnowledgeNodeType recordType, UUID recordId, UUID ownerId) {
        this(id, knowledgeEdgeId, recordType, recordId);
        this.ownerId = ownerId;
    }

    public UUID getOwnerId() { return ownerId; }
    public UUID getId() { return id; }
    public UUID getKnowledgeEdgeId() { return knowledgeEdgeId; }
    public KnowledgeNodeType getRecordType() { return recordType; }
    public UUID getRecordId() { return recordId; }
}
