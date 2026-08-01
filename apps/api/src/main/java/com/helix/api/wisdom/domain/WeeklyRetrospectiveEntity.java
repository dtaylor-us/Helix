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
@Table(name = "weekly_retrospectives")
public class WeeklyRetrospectiveEntity {

    @Id
    private UUID id;

    @Column(name = "period_start", nullable = false)
    private OffsetDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private OffsetDateTime periodEnd;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(nullable = false, columnDefinition = "text")
    private String assistance;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RetrospectiveSource source;

    @Column(name = "ai_provider", length = 50)
    private String aiProvider;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    protected WeeklyRetrospectiveEntity() {}

    /**
     * Legacy constructor, preserved for existing callers/tests. Defaults {@code source} to
     * DETERMINISTIC with no provider/model, matching the pre-ADR-016 behavior of this constructor.
     */
    public WeeklyRetrospectiveEntity(UUID id, OffsetDateTime periodStart, OffsetDateTime periodEnd, String summary,
                                     String assistance, OffsetDateTime createdAt) {
        this(id, periodStart, periodEnd, summary, assistance, createdAt, RetrospectiveSource.DETERMINISTIC, null, null);
    }

    public WeeklyRetrospectiveEntity(UUID id, OffsetDateTime periodStart, OffsetDateTime periodEnd, String summary,
                                     String assistance, OffsetDateTime createdAt, RetrospectiveSource source,
                                     String aiProvider, String aiModel) {
        this.id = id;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.summary = summary;
        this.assistance = assistance;
        this.createdAt = createdAt;
        this.source = source;
        this.aiProvider = aiProvider;
        this.aiModel = aiModel;
    }

    public UUID getId() { return id; }
    public OffsetDateTime getPeriodStart() { return periodStart; }
    public OffsetDateTime getPeriodEnd() { return periodEnd; }
    public String getSummary() { return summary; }
    public String getAssistance() { return assistance; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public RetrospectiveSource getSource() { return source; }
    public String getAiProvider() { return aiProvider; }
    public String getAiModel() { return aiModel; }
}
