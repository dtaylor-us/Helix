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
 * Onboarding progress, one row per user. Previously a single fixed-id singleton row under the
 * single-user assumption (see V10 migration's own comment anticipating this change) — ADR-021's V12
 * migration re-keyed it to {@code owner_id} instead, so each invited user gets an independent
 * onboarding journey.
 */
@Entity
@Table(name = "onboarding_state")
public class OnboardingStateEntity {

    /** Historical fixed id from the pre-ADR-021 singleton row; kept only as a fixture constant for
     * existing tests (never persisted under this id anymore -- see {@code owner_id}). */
    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OnboardingStatus status;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ADR-021: nullable here only so pre-existing test fixtures (never persisted) keep compiling.
    // The database column is NOT NULL -- see TransformationEntity.ownerId for the full rationale.
    @Column(name = "owner_id")
    private UUID ownerId;

    protected OnboardingStateEntity() {}

    /** Legacy constructor, preserved for existing test fixtures. Leaves ownerId unset. */
    public OnboardingStateEntity(UUID id, OnboardingStatus status, OffsetDateTime updatedAt) {
        this.id = id;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public OnboardingStateEntity(UUID id, OnboardingStatus status, OffsetDateTime updatedAt, UUID ownerId) {
        this(id, status, updatedAt);
        this.ownerId = ownerId;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
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
