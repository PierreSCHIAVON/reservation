package com.example.reservation.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception levée lorsque les données fournies par l'utilisateur sont invalides.
 *
 * <p>Remplace IllegalArgumentException pour une meilleure sémantique business.
 *
 * <p>Exemples:
 * <ul>
 *   <li>Discounted price exceeds original price</li>
 *   <li>Invalid date range</li>
 *   <li>Missing required field</li>
 * </ul>
 */
public class InvalidInputException extends ReservationApplicationException {

    private final String fieldName;

    public InvalidInputException(String message) {
        super(message);
        this.fieldName = null;
    }

    public InvalidInputException(String fieldName, String message) {
        super(String.format("Invalid value for '%s': %s", fieldName, message));
        this.fieldName = fieldName;
    }

    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
        this.fieldName = null;
    }

    @Override
    public int getHttpStatus() {
        return HttpStatus.BAD_REQUEST.value();
    }

    @Override
    public String getErrorCode() {
        return "INVALID_INPUT";
    }

    public String getFieldName() {
        return fieldName;
    }
}
