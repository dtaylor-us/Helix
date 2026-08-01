package com.helix.api.identity.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void shouldBuildCorsConfigurationFromAllowedOrigins() {
        SecurityConfig securityConfig = new SecurityConfig();

        CorsConfigurationSource source = securityConfig.corsConfigurationSource(
            List.of("http://localhost:5173", "http://localhost:4173")
        );
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/today");
        request.addHeader("Origin", "http://localhost:5173");
        request.addHeader("Access-Control-Request-Method", "GET");

        CorsConfiguration corsConfiguration = source.getCorsConfiguration(request);

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.getAllowedOrigins())
            .containsExactly("http://localhost:5173", "http://localhost:4173");
        assertThat(corsConfiguration.getAllowedMethods())
            .contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(corsConfiguration.getAllowedHeaders()).contains("*");
    }

    @Test
    void shouldTrimAndIgnoreBlankCorsOrigins() {
        SecurityConfig securityConfig = new SecurityConfig();

        CorsConfigurationSource source = securityConfig.corsConfigurationSource(
            List.of(" http://localhost:5173 ", "  ", "http://localhost:4173")
        );
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/today");

        CorsConfiguration corsConfiguration = source.getCorsConfiguration(request);

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.getAllowedOrigins())
            .containsExactly("http://localhost:5173", "http://localhost:4173");
    }
}
