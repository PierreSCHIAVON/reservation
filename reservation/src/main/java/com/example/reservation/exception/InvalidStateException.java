package com.example.reservation.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception levée lorsqu'une opération ne peut pas être effectuée
 * en raison de l'état actuel de la ressource.
 *
 * <p>Remplace IllegalStateException pour une meilleure sémantique business.
 *
 * <p>Exemples:
 * <ul>
 *   <li>Cannot activate an already active property</li>
 *   <li>Cannot confirm a non-pending reservation</li>
 *   <li>Cannot cancel a completed reservation</li>
 *   <li>Access code is not active</li>
 * </ul>
 */
public class InvalidStateException extends ReservationApplicationException {

    public InvalidStateException(String message) {
        super(message);
    }

    public InvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getHttpStatus() {
        return HttpStatus.CONFLICT.value();
    }

    @Override
    public String getErrorCode() {
        return "INVALID_STATE";
    }
}
