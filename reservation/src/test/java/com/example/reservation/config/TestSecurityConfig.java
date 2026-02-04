package com.example.reservation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Map;

/**
 * Test configuration that provides a mock JwtDecoder for integration tests.
 * This allows tests to run without requiring a real Keycloak instance.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        // Return a mock decoder that creates a JWT from the token value
        return token -> {
            // The token value is used as the subject for simplicity in tests
            return Jwt.withTokenValue(token)
                    .header("alg", "RS256")
                    .header("typ", "JWT")
                    .claim("sub", token)
                    .claim("iss", "http://localhost:8081/realms/reservation")
                    .claim("aud", "reservation-test")
                    .claim("exp", Instant.now().plusSeconds(3600))
                    .claim("iat", Instant.now())
                    .claim("realm_access", Map.of("roles", java.util.List.of("USER", "OWNER")))
                    .build();
        };
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
