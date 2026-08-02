package com.helix.api.identity.application;

import com.helix.api.identity.adapter.out.persistence.AuthorizedUserRepository;
import com.helix.api.identity.adapter.out.persistence.UserRepository;
import com.helix.api.identity.domain.UserEntity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * After Google authenticates a user, this decides whether Helix lets them in (ADR-021):
 * 1. Their email must already be on the {@code authorized_users} allowlist — Helix is invite-only,
 *    there is no self-service signup.
 * 2. If allowed, find-or-create the local {@link UserEntity} and wrap it as a {@link HelixOidcUser}
 *    carrying the internal user id every owner-scoped query keys off.
 *
 * Matching is by {@code google_sub} first (the stable identifier for returning users), falling back
 * to {@code email} only for a user's very first login (this is also how the migration's bootstrap
 * account — created with a placeholder {@code google_sub} — gets bound to the real Google account on
 * its first login).
 */
@Service
public class HelixOidcUserService extends OidcUserService {

    private final AuthorizedUserRepository authorizedUserRepository;
    private final UserRepository userRepository;

    public HelixOidcUserService(AuthorizedUserRepository authorizedUserRepository, UserRepository userRepository) {
        this.authorizedUserRepository = authorizedUserRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser delegate = super.loadUser(userRequest);

        String email = delegate.getEmail();
        String googleSub = delegate.getSubject();

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("email_not_available"), "Google did not return an email address.");
        }

        UserEntity user = userRepository.findByGoogleSub(googleSub)
            .or(() -> userRepository.findByEmail(email))
            .orElseGet(() -> provisionIfAuthorized(email, googleSub));

        // Keep the record's google_sub/display name current -- covers both the bootstrap
        // placeholder-sub row and a real user's display name changing on Google's side.
        user.confirmGoogleSub(googleSub);
        if (delegate.getFullName() != null) {
            user.updateDisplayName(delegate.getFullName());
        }
        userRepository.save(user);

        return new HelixOidcUser(user.getId(), delegate.getIdToken(), delegate.getUserInfo());
    }

    private UserEntity provisionIfAuthorized(String email, String googleSub) {
        if (!authorizedUserRepository.existsById(email)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error("not_invited"), "This Google account is not on the Helix invite list.");
        }
        return userRepository.save(new UserEntity(UUID.randomUUID(), email, null, googleSub, OffsetDateTime.now()));
    }

    // Explicit widening cast target for Spring's userInfoEndpoint().oidcUserService(...) DSL, which
    // expects an OAuth2UserService<OidcUserRequest, OidcUser> reference.
    public OAuth2UserService<OidcUserRequest, OidcUser> asService() {
        return this;
    }
}
