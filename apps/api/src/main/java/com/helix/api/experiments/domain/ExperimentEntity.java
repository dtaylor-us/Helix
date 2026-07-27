package com.helix.api.experiments.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "experiments")
public class ExperimentEntity {

    @Id
    private UUID id;

    @Column(name = "transformation_id", nullable = false)
    private UUID transformationId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(columnDefinition = "text")
    private String hypothesis;

    @Column(name = "next_action", columnDefinition = "text")
    private String nextAction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExperimentStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ExperimentEntity() {}

    public ExperimentEntity(UUID id, UUID transformationId, String title, String hypothesis, String nextAction,
                            ExperimentStatus status, OffsetDateTime createdAt) {
        this.id = id;
        this.transformationId = transformationId;
        this.title = title;
        this.hypothesis = hypothesis;
        this.nextAction = nextAction;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getTransformationId() { return transformationId; }
    public String getTitle() { return title; }
    public String getHypothesis() { return hypothesis; }
    public String getNextAction() { return nextAction; }
    public ExperimentStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
