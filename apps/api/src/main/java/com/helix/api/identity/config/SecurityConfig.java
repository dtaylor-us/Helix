package com.helix.api.identity.config;

import com.helix.api.identity.application.HelixOidcUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * ADR-021: Google OAuth2/OIDC login, session-cookie auth. Every {@code /api/v1/**} route requires an
 * authenticated session except health and the "who am I" check (which itself returns 401 in its body
 * rather than needing the filter chain to gate it — see {@code AuthController}).
 *
 * CSRF is intentionally left disabled (as it was pre-ADR-021), not because it no longer matters now
 * that session cookies exist, but because two other controls already cover the same risk for this
 * specific API: (1) the session cookie is issued with {@code SameSite=Lax}, so it isn't attached to
 * cross-site requests that aren't a top-level GET navigation; (2) every mutating endpoint requires
 * {@code Content-Type: application/json}, which forces a CORS preflight that this app's strict
 * {@code allowed-origins} allowlist will reject for any origin that isn't the configured frontend.
 * Revisit if a mutating endpoint is ever added that accepts a "simple request" content type.
 */
@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, HelixOidcUserService oidcUserService,
                                     @Value("${helix.web.app-url:http://localhost:5173}") String appUrl) throws Exception {
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
                .defaultSuccessUrl(appUrl, true)
                .failureUrl(appUrl + "/login?error=not_invited")
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/api/v1/auth/logout", "POST"))
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_NO_CONTENT))
            )
            .exceptionHandling(exceptions -> exceptions
                // Unauthenticated calls to the JSON API get a plain 401 (for the SPA's fetch calls to
                // react to), not a redirect to Google's login page -- the login redirect only makes
                // sense for a top-level browser navigation, which the SPA itself initiates explicitly
                // via a link to /oauth2/authorization/google, not via a failed fetch.
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED),
                    new AntPathRequestMatcher("/api/**")
                )
            )
            .build();
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
