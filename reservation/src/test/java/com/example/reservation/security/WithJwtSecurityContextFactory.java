package com.example.reservation.security;

import com.example.reservation.config.KeycloakRolesExtractor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating a SecurityContext with a JWT authentication token.
 * Used by the {@link WithJwt} annotation.
 */
public class WithJwtSecurityContextFactory implements WithSecurityContextFactory<WithJwt> {

    @Override
    public SecurityContext createSecurityContext(WithJwt annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .claim("sub", annotation.subject())
                .claim("email", annotation.email())
                .claim("preferred_username", annotation.username())
                .claim("iss", "http://localhost:8081/realms/reservation")
                .claim("aud", "reservation-test")
                .claim("exp", Instant.now().plusSeconds(3600))
                .claim("iat", Instant.now())
                .claim("realm_access", Map.of("roles", List.of(annotation.roles())))
                .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                KeycloakRolesExtractor.extractRealmRoles(jwt)
        );

        context.setAuthentication(authentication);
        return context;
    }
}
