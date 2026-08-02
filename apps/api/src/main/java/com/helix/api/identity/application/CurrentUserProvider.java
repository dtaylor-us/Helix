package com.helix.api.identity.application;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Single point of truth for "who is making this request" (ADR-021). Every owner-scoped service
 * method should resolve the caller through this, not by reading {@code SecurityContextHolder}
 * directly — keeps the "how do we identify the current user" decision in one place.
 */
@Component
public class CurrentUserProvider {

    public UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof HelixOidcUser principal)) {
            // Should be unreachable in practice: SecurityConfig requires authentication on every
            // route that resolves a CurrentUserProvider. Failing loudly here is intentional -- a
            // service accidentally reachable without authentication is exactly the bug this class
            // exists to prevent from failing silently (e.g. by falling back to some default user).
            throw new IllegalStateException("No authenticated Helix user in the current security context.");
        }
        return principal.getUserId();
    }
}
