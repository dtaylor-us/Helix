package com.helix.api.suggestions.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "suggestions")
public class SuggestionEntity {

    @Id
    private UUID id;

    @Column(name = "experiment_id", nullable = false)
    private UUID experimentId;

    @Column(name = "reflection_id")
    private UUID reflectionId;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SuggestionStatus status;

    @Column(name = "replacement_text", columnDefinition = "text")
    private String replacementText;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    protected SuggestionEntity() {}

    public SuggestionEntity(UUID id, UUID experimentId, UUID reflectionId, String text, SuggestionStatus status,
                            String replacementText, OffsetDateTime createdAt, OffsetDateTime respondedAt) {
        this.id = id;
        this.experimentId = experimentId;
        this.reflectionId = reflectionId;
        this.text = text;
        this.status = status;
        this.replacementText = replacementText;
        this.createdAt = createdAt;
        this.respondedAt = respondedAt;
    }

    public UUID getId() { return id; }
    public UUID getExperimentId() { return experimentId; }
    public UUID getReflectionId() { return reflectionId; }
    public String getText() { return text; }
    public SuggestionStatus getStatus() { return status; }
    public String getReplacementText() { return replacementText; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getRespondedAt() { return respondedAt; }

    public void accept() {
        this.status = SuggestionStatus.ACCEPTED;
        this.respondedAt = OffsetDateTime.now();
    }

    public void dismiss() {
        this.status = SuggestionStatus.DISMISSED;
        this.respondedAt = OffsetDateTime.now();
    }

    public void replaceWith(String replacement) {
        this.status = SuggestionStatus.REPLACED;
        this.replacementText = replacement;
        this.respondedAt = OffsetDateTime.now();
    }
}
