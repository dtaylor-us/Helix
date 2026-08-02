package com.helix.api.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A Helix account. Created lazily on a user's first successful Google login (see
 * {@code HelixOidcUserService}) — there is no separate sign-up flow, since access is invite-only
 * (see {@link AuthorizedUserEntity}) and Google is the sole identity provider (ADR-021).
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "google_sub", nullable = false, unique = true)
    private String googleSub;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected UserEntity() {}

    public UserEntity(UUID id, String email, String displayName, String googleSub, OffsetDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.googleSub = googleSub;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getGoogleSub() { return googleSub; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    /**
     * The bootstrap account's {@code google_sub} is a placeholder until its first real login (see
     * V12 migration) — this records the real Google subject id once it's known, so later logins can
     * be matched by {@code google_sub} instead of by email (email match is only used for the very
     * first login / allowlist check, per ADR-021).
     */
    public void confirmGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
