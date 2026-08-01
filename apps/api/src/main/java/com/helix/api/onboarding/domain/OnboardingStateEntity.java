package com.helix.api.onboarding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Singleton row tracking onboarding progress. Helix is currently single-user with no auth
 * (ADR-013 defers auth behind a port), so there is exactly one row, addressed by
 * {@link #SINGLETON_ID}, rather than one per user — the same shape auth would later key by
 * user_id.
 */
@Entity
@Table(name = "onboarding_state")
public class OnboardingStateEntity {

    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OnboardingStatus status;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected OnboardingStateEntity() {}

    public OnboardingStateEntity(UUID id, OnboardingStatus status, OffsetDateTime updatedAt) {
        this.id = id;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public OnboardingStatus getStatus() { return status; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void advanceTo(OnboardingStatus next, OffsetDateTime now) {
        // Monotonic: never move backward. Ordinal comparison is safe because the enum is declared
        // in strict progression order (NOT_STARTED -> FIRST_TRANSFORMATION_CREATED -> COMPLETE).
        if (next.ordinal() > this.status.ordinal()) {
            this.status = next;
            this.updatedAt = now;
        }
    }

    /**
     * Unconditionally resets to NOT_STARTED, bypassing the monotonic guard in {@link #advanceTo}.
     * Used only by the whole-app data wipe (ADR-019) — an intentional full-system reset, not a
     * normal regression, so the usual "never move backward" rule doesn't apply here.
     */
    public void reset(OffsetDateTime now) {
        this.status = OnboardingStatus.NOT_STARTED;
        this.updatedAt = now;
    }
}
