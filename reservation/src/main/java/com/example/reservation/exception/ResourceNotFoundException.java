package com.example.reservation.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception levée lorsqu'une ressource demandée n'existe pas.
 *
 * <p>Remplace EntityNotFoundException pour une meilleure cohérence
 * et pour éviter la dépendance à JPA dans la couche business.
 *
 * <p>Exemples:
 * <ul>
 *   <li>Property not found</li>
 *   <li>Reservation not found</li>
 *   <li>Access code not found</li>
 * </ul>
 */
public class ResourceNotFoundException extends ReservationApplicationException {

    private final String resourceType;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s not found: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceType = "Resource";
        this.resourceId = "unknown";
    }

    @Override
    public int getHttpStatus() {
        return HttpStatus.NOT_FOUND.value();
    }

    @Override
    public String getErrorCode() {
        return "RESOURCE_NOT_FOUND";
    }

    public String getResourceType() {
        return resourceType;
    }

    public Object getResourceId() {
        return resourceId;
    }
}
