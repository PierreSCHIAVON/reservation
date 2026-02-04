package com.example.reservation.exception;

/**
 * Base exception class for all business exceptions in the reservation application.
 *
 * <p>This exception serves as the parent for all custom exceptions in the application,
 * allowing for easier exception handling and consistent error reporting.
 */
public abstract class ReservationApplicationException extends RuntimeException {

    protected ReservationApplicationException(String message) {
        super(message);
    }

    protected ReservationApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns the HTTP status code that should be returned for this exception.
     * Subclasses should override this method to specify the appropriate status code.
     */
    public abstract int getHttpStatus();

    /**
     * Returns a machine-readable error code for this exception.
     * Useful for API clients to programmatically handle specific errors.
     */
    public abstract String getErrorCode();
}
