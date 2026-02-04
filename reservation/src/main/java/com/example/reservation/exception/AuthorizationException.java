package com.example.reservation.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception levée lorsqu'un utilisateur tente d'accéder à une ressource
 * sans avoir les permissions nécessaires.
 *
 * <p>Complète AccessDeniedException de Spring Security avec une sémantique business.
 *
 * <p>Exemples:
 * <ul>
 *   <li>User is not the property owner</li>
 *   <li>User is not the reservation tenant</li>
 *   <li>User is not the access code creator</li>
 * </ul>
 */
public class AuthorizationException extends ReservationApplicationException {

    private final String requiredPermission;

    public AuthorizationException(String message) {
        super(message);
        this.requiredPermission = null;
    }

    public AuthorizationException(String message, String requiredPermission) {
        super(message);
        this.requiredPermission = requiredPermission;
    }

    @Override
    public int getHttpStatus() {
        return HttpStatus.FORBIDDEN.value();
    }

    @Override
    public String getErrorCode() {
        return "AUTHORIZATION_FAILED";
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }
}
