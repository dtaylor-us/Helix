package com.helix.api.wisdom.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    protected WeeklyRetrospectiveEntity() {}

    public WeeklyRetrospectiveEntity(UUID id, OffsetDateTime periodStart, OffsetDateTime periodEnd, String summary,
                                     String assistance, OffsetDateTime createdAt) {
        this.id = id;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.summary = summary;
        this.assistance = assistance;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public OffsetDateTime getPeriodStart() { return periodStart; }
    public OffsetDateTime getPeriodEnd() { return periodEnd; }
    public String getSummary() { return summary; }
    public String getAssistance() { return assistance; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
