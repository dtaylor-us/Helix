package com.helix.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HelixApiApplicationTests {

    @Test
    void applicationClassExists() {
        assertNotNull(HelixApiApplication.class);
    }

    @Test
    void generatedSecurityUserDetailsAutoConfigurationIsExcludedForAllProfiles() {
        SpringBootApplication annotation = HelixApiApplication.class.getAnnotation(SpringBootApplication.class);

        assertNotNull(annotation);
        assertTrue(
            Arrays.asList(annotation.exclude()).contains(UserDetailsServiceAutoConfiguration.class)
        );
    }
}
