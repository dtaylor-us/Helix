package com.helix.api.identity.config;

import com.helix.api.identity.application.HelixOidcUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * ADR-021: Google OAuth2/OIDC login, session-cookie auth. Every {@code /api/v1/**} route requires an
 * authenticated session except health and the "who am I" check (which itself returns 401 in its body
 * rather than needing the filter chain to gate it — see {@code AuthController}).
 *
 * CSRF is intentionally left disabled (as it was pre-ADR-021). The primary control this relies on
 * holds regardless of deployment topology: every mutating endpoint requires
 * {@code Content-Type: application/json}, which is not a CORS "simple request" -- it forces a
 * preflight, and this app's strict {@code allowed-origins} allowlist rejects the preflight outright
 * for any origin that isn't the configured frontend. A cross-site form/img/script-tag attack (the
 * classic CSRF vector) cannot set a JSON content-type, so it never gets far enough to attach the
 * session cookie in the first place. {@code SameSite} on the session cookie (see
 * {@code application.properties}' {@code HELIX_SESSION_COOKIE_SAMESITE}) is defense-in-depth on top
 * of that, not the load-bearing control -- which matters because ADR-022's split-origin production
 * deployment (frontend and API on different domains) requires {@code SameSite=None} to work at all,
 * unlike same-origin/same-site local dev's {@code SameSite=Lax}. Revisit if a mutating endpoint is
 * ever added that accepts a "simple request" content type.
 */
@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, HelixOidcUserService oidcUserService,
                                     @Value("${helix.web.app-url:http://localhost:5173}") String appUrl) throws Exception {
        String normalizedAppUrl = normalizeAppUrl(appUrl);

        return http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/health", "/api/v1/auth/me").permitAll()
                .requestMatchers("/oauth2/**", "/login/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService.asService()))
                .defaultSuccessUrl(normalizedAppUrl, true)
                .failureUrl(normalizedAppUrl + "/login?error=not_invited")
            )
            .logout(logout -> logout
                .logoutRequestMatcher(PathPatternRequestMatcher.pathPattern(
                    HttpMethod.POST, "/api/v1/auth/logout"
                ))
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_NO_CONTENT))
            )
            .exceptionHandling(exceptions -> exceptions
                // Unauthenticated calls to the JSON API get a plain 401 (for the SPA's fetch calls to
                // react to), not a redirect to Google's login page -- the login redirect only makes
                // sense for a top-level browser navigation, which the SPA itself initiates explicitly
                // via a link to /oauth2/authorization/google, not via a failed fetch.
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED),
                    PathPatternRequestMatcher.pathPattern("/api/**")
                )
            )
            .build();
    }

    static String normalizeAppUrl(String appUrl) {
        String normalized = appUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
        @Value("${helix.web.allowed-origins:http://localhost:5173}") List<String> allowedOrigins
    ) {
        List<String> normalizedOrigins = allowedOrigins
            .stream()
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(normalizedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // Required so the SPA's session cookie is actually sent/accepted cross-origin (e.g. local dev
        // where the Vite dev server on :5173 calls the API on :8080) -- without this, credentialed
        // fetch requests are silently rejected by the browser regardless of server-side CORS headers.
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
