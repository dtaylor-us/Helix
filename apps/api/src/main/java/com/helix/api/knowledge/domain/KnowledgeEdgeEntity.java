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
@Table(name = "knowledge_edge")
public class KnowledgeEdgeEntity {

    @Id
    private UUID id;

    @Column(name = "source_node_id", nullable = false)
    private UUID sourceNodeId;

    @Column(name = "target_node_id", nullable = false)
    private UUID targetNodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 64)
    private KnowledgeEdgeType relationshipType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private KnowledgeEdgeOrigin origin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private KnowledgeEdgeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KnowledgeEdgeConfidence confidence;

    @Column(nullable = false, columnDefinition = "text")
    private String explanation;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "effective_from")
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    @Column(name = "ai_invocation_id")
    private UUID aiInvocationId;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "superseded_by_edge_id")
    private UUID supersededByEdgeId;

    // ADR-021 gap: NOT YET set by KnowledgeGraphProjectionService -- see KnowledgeNodeEntity.ownerId.
    @Column(name = "owner_id")
    private UUID ownerId;

    protected KnowledgeEdgeEntity() {}

    public KnowledgeEdgeEntity(
        UUID id, UUID sourceNodeId, UUID targetNodeId, KnowledgeEdgeType relationshipType,
        KnowledgeEdgeOrigin origin, KnowledgeEdgeStatus status, KnowledgeEdgeConfidence confidence,
        String explanation, OffsetDateTime createdAt
    ) {
        this.id = id;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.relationshipType = relationshipType;
        this.origin = origin;
        this.status = status;
        this.confidence = confidence;
        this.explanation = explanation;
        this.createdAt = createdAt;
        this.confirmedAt = status == KnowledgeEdgeStatus.CONFIRMED ? createdAt : null;
    }

    public KnowledgeEdgeEntity(
        UUID id, UUID sourceNodeId, UUID targetNodeId, KnowledgeEdgeType relationshipType,
        KnowledgeEdgeOrigin origin, KnowledgeEdgeStatus status, KnowledgeEdgeConfidence confidence,
        String explanation, OffsetDateTime createdAt, UUID ownerId
    ) {
        this(id, sourceNodeId, targetNodeId, relationshipType, origin, status, confidence, explanation, createdAt);
        this.ownerId = ownerId;
    }

    public UUID getOwnerId() { return ownerId; }
    public UUID getId() { return id; }
    public UUID getSourceNodeId() { return sourceNodeId; }
    public UUID getTargetNodeId() { return targetNodeId; }
    public KnowledgeEdgeType getRelationshipType() { return relationshipType; }
    public KnowledgeEdgeOrigin getOrigin() { return origin; }
    public KnowledgeEdgeStatus getStatus() { return status; }
    public KnowledgeEdgeConfidence getConfidence() { return confidence; }
    public String getExplanation() { return explanation; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getEffectiveFrom() { return effectiveFrom; }
    public OffsetDateTime getEffectiveTo() { return effectiveTo; }
    public UUID getAiInvocationId() { return aiInvocationId; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public OffsetDateTime getRejectedAt() { return rejectedAt; }
    public UUID getSupersededByEdgeId() { return supersededByEdgeId; }

    /** Phase 11D governance action. Only meaningful for PROPOSED edges (AI_PROPOSED/USER_CREATED origin). */
    public void confirm(OffsetDateTime now) {
        this.status = KnowledgeEdgeStatus.CONFIRMED;
        this.confirmedAt = now;
    }

    /** Phase 11D governance action. */
    public void reject(OffsetDateTime now) {
        this.status = KnowledgeEdgeStatus.REJECTED;
        this.rejectedAt = now;
    }

    /** Phase 11D governance action: hide from default views without asserting it's wrong. */
    public void hide() {
        this.status = KnowledgeEdgeStatus.HIDDEN;
    }
}
