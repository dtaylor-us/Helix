package com.helix.api.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * An invite-only allowlist entry (ADR-021). Helix is not a self-service-signup product — a row here
 * must exist for an email before Google login is permitted to create/reuse a {@link UserEntity} for
 * it. Managed as data (insert a row to invite someone) rather than as config, so inviting a person
 * doesn't require a redeploy.
 */
@Entity
@Table(name = "authorized_users")
public class AuthorizedUserEntity {

    @Id
    private String email;

    @Column(name = "invited_at", nullable = false)
    private OffsetDateTime invitedAt;

    private String note;

    protected AuthorizedUserEntity() {}

    public AuthorizedUserEntity(String email, OffsetDateTime invitedAt, String note) {
        this.email = email;
        this.invitedAt = invitedAt;
        this.note = note;
    }

    public String getEmail() { return email; }
    public OffsetDateTime getInvitedAt() { return invitedAt; }
    public String getNote() { return note; }
}
