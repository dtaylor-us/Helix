package com.helix.api.identity.application;

import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.UUID;

/**
 * Wraps Spring Security's default OIDC principal with the Helix-internal {@link UUID} that every
 * owner-scoped repository query keys off (ADR-021) — the Google subject id ("sub") is never used
 * directly as a data-ownership key, since it's an external identifier we don't control.
 */
public class HelixOidcUser extends DefaultOidcUser {

    private final UUID userId;

    public HelixOidcUser(UUID userId, OidcIdToken idToken, OidcUserInfo userInfo) {
        super(java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")),
            idToken, userInfo);
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
