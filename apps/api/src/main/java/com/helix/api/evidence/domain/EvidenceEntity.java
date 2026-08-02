package com.helix.api.evidence.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "evidence")
public class EvidenceEntity {

    @Id
    private UUID id;

    @Column(name = "belief_id", nullable = false)
    private UUID beliefId;

    @Column(name = "experiment_id")
    private UUID experimentId;

    @Column(name = "reflection_id")
    private UUID reflectionId;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(columnDefinition = "text")
    private String interpretation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EvidenceDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "provenance_source_kind", nullable = false, length = 32)
    private ProvenanceSourceKind provenanceSourceKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "provenance_record_type", nullable = false, length = 32)
    private ProvenanceRecordType provenanceRecordType;

    @Column(name = "provenance_record_id")
    private UUID provenanceRecordId;

    @Column(name = "provenance_excerpt", columnDefinition = "text")
    private String provenanceExcerpt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // ADR-021: nullable here only so pre-existing test fixtures (never persisted) keep compiling.
    // The database column is NOT NULL -- see TransformationEntity.ownerId for the full rationale.
    @Column(name = "owner_id")
    private UUID ownerId;

    protected EvidenceEntity() {}

    public EvidenceEntity(UUID id, UUID beliefId, UUID experimentId, UUID reflectionId, String summary,
                          String interpretation, EvidenceDirection direction, ProvenanceSourceKind provenanceSourceKind,
                          ProvenanceRecordType provenanceRecordType, UUID provenanceRecordId,
                          String provenanceExcerpt, OffsetDateTime createdAt) {
        this.id = id;
        this.beliefId = beliefId;
        this.experimentId = experimentId;
        this.reflectionId = reflectionId;
        this.summary = summary;
        this.interpretation = interpretation;
        this.direction = direction;
        this.provenanceSourceKind = provenanceSourceKind;
        this.provenanceRecordType = provenanceRecordType;
        this.provenanceRecordId = provenanceRecordId;
        this.provenanceExcerpt = provenanceExcerpt;
        this.createdAt = createdAt;
    }

    public EvidenceEntity(UUID id, UUID beliefId, UUID experimentId, UUID reflectionId, String summary,
                          String interpretation, EvidenceDirection direction, ProvenanceSourceKind provenanceSourceKind,
                          ProvenanceRecordType provenanceRecordType, UUID provenanceRecordId,
                          String provenanceExcerpt, OffsetDateTime createdAt, UUID ownerId) {
        this(id, beliefId, experimentId, reflectionId, summary, interpretation, direction, provenanceSourceKind,
            provenanceRecordType, provenanceRecordId, provenanceExcerpt, createdAt);
        this.ownerId = ownerId;
    }

    public UUID getId() { return id; }
    public UUID getBeliefId() { return beliefId; }
    public UUID getExperimentId() { return experimentId; }
    public UUID getReflectionId() { return reflectionId; }
    public String getSummary() { return summary; }
    public String getInterpretation() { return interpretation; }
    public EvidenceDirection getDirection() { return direction; }
    public ProvenanceSourceKind getProvenanceSourceKind() { return provenanceSourceKind; }
    public ProvenanceRecordType getProvenanceRecordType() { return provenanceRecordType; }
    public UUID getProvenanceRecordId() { return provenanceRecordId; }
    public String getProvenanceExcerpt() { return provenanceExcerpt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public UUID getOwnerId() { return ownerId; }
}