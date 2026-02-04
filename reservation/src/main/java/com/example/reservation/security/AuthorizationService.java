package com.example.reservation.security;

import com.example.reservation.repository.PropertyAccessCodeRepository;
import com.example.reservation.repository.PropertyRepository;
import com.example.reservation.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service d'autorisation utilisé par @PreAuthorize pour vérifier les permissions.
 * Les méthodes retournent true si l'utilisateur a la permission, false sinon.
 */
@Service("authz")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationService {

    private final PropertyRepository propertyRepository;
    private final ReservationRepository reservationRepository;
    private final PropertyAccessCodeRepository accessCodeRepository;

    /**
     * Vérifie si l'utilisateur est propriétaire de la propriété.
     */
    public boolean isPropertyOwner(UUID propertyId, String userSub) {
        requirePropertyExists(propertyId);
        return propertyRepository.existsByIdAndOwnerSub(propertyId, userSub);
    }

    /**
     * Vérifie si l'utilisateur est le locataire de la réservation.
     */
    public boolean isReservationTenant(UUID reservationId, String userSub) {
        requireReservationExists(reservationId);
        return reservationRepository.existsByIdAndTenantSub(reservationId, userSub);
    }

    /**
     * Vérifie si l'utilisateur est propriétaire du bien lié à la réservation.
     */
    public boolean isReservationPropertyOwner(UUID reservationId, String userSub) {
        requireReservationExists(reservationId);
        return reservationRepository.existsByIdAndPropertyOwnerSub(reservationId, userSub);
    }

    /**
     * Vérifie si l'utilisateur peut accéder à la réservation (locataire ou propriétaire).
     */
    public boolean canAccessReservation(UUID reservationId, String userSub) {
        requireReservationExists(reservationId);
        return reservationRepository.existsByIdAndTenantSub(reservationId, userSub)
                || reservationRepository.existsByIdAndPropertyOwnerSub(reservationId, userSub);
    }

    /**
     * Vérifie si l'utilisateur est le créateur du code d'accès.
     */
    public boolean isAccessCodeCreator(UUID accessCodeId, String userSub) {
        requireAccessCodeExists(accessCodeId);
        return accessCodeRepository.existsByIdAndCreatedBySub(accessCodeId, userSub);
    }

    private void requirePropertyExists(UUID propertyId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new EntityNotFoundException("Property not found: " + propertyId);
        }
    }

    private void requireReservationExists(UUID reservationId) {
        if (!reservationRepository.existsById(reservationId)) {
            throw new EntityNotFoundException("Reservation not found: " + reservationId);
        }
    }

    private void requireAccessCodeExists(UUID accessCodeId) {
        if (!accessCodeRepository.existsById(accessCodeId)) {
            throw new EntityNotFoundException("Access code not found: " + accessCodeId);
        }
    }
}
