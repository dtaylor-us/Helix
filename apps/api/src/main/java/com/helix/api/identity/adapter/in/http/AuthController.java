package com.helix.api.identity.adapter.in.http;

import com.helix.api.identity.application.HelixOidcUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Session/identity endpoints for the SPA (ADR-021). Login itself is Spring Security's built-in
 * {@code /oauth2/authorization/google} redirect (not handled here); logout is the built-in
 * {@code POST /api/v1/auth/logout} configured in {@code SecurityConfig}. This controller only
 * answers "who am I" so the frontend can decide whether to show the app or a login prompt.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<CurrentUserDto> me(@AuthenticationPrincipal HelixOidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new CurrentUserDto(principal.getUserId(), principal.getEmail(), principal.getFullName()));
    }

    public record CurrentUserDto(UUID id, String email, String displayName) {}
}
