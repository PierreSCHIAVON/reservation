package com.example.reservation.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.*;

/**
 * Annotation to simulate a JWT-authenticated user in tests.
 *
 * Usage:
 * <pre>
 * &#64;Test
 * &#64;WithJwt(subject = "user-123", email = "user@example.com", roles = {"USER", "OWNER"})
 * void testAuthenticatedEndpoint() {
 *     // test code
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WithSecurityContext(factory = WithJwtSecurityContextFactory.class)
public @interface WithJwt {

    /**
     * The subject (sub claim) of the JWT, typically the Keycloak user ID.
     */
    String subject() default "test-user-sub";

    /**
     * The email claim of the JWT.
     */
    String email() default "testuser@example.com";

    /**
     * The roles assigned to the user.
     */
    String[] roles() default {"USER", "OWNER"};

    /**
     * The preferred username claim.
     */
    String username() default "testuser";
}
