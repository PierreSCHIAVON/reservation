package com.example.reservation.service;

import com.example.reservation.domain.property.Property;
import com.example.reservation.domain.reservation.PricingType;
import com.example.reservation.domain.reservation.Reservation;
import com.example.reservation.domain.reservation.ReservationStatus;
import com.example.reservation.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final PropertyService propertyService;

    public Reservation findById(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + id));
    }

    public List<Reservation> findByTenant(String tenantSub) {
        return reservationRepository.findByTenantSub(tenantSub);
    }

    public List<Reservation> findByProperty(UUID propertyId) {
        return reservationRepository.findByPropertyId(propertyId);
    }

    public List<Reservation> findByPropertyOwner(String ownerSub) {
        return reservationRepository.findByPropertyOwnerSub(ownerSub);
    }

    public List<Reservation> findPendingByPropertyOwner(String ownerSub) {
        return reservationRepository.findByPropertyOwnerSubAndStatus(ownerSub, ReservationStatus.PENDING);
    }

    @Transactional
    public Reservation create(UUID propertyId, String tenantSub, LocalDate startDate, LocalDate endDate) {
        Property property = propertyService.findById(propertyId);

        if (!property.isBookable()) {
            throw new IllegalStateException("Cette propriété n'est pas disponible à la réservation");
        }

        // Vérifier les chevauchements
        List<Reservation> overlapping = reservationRepository.findOverlappingReservations(propertyId, startDate, endDate);
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("Les dates demandées chevauchent une réservation existante");
        }

        // Calculer le prix
        long nights = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        BigDecimal totalPrice = property.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        Reservation reservation = Reservation.builder()
                .property(property)
                .tenantSub(tenantSub)
                .startDate(startDate)
                .endDate(endDate)
                .status(ReservationStatus.PENDING)
                .unitPriceApplied(property.getPricePerNight())
                .totalPrice(totalPrice)
                .pricingType(PricingType.NORMAL)
                .build();

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation confirm(UUID id) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Seule une réservation PENDING peut être confirmée");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation cancel(UUID id) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("La réservation est déjà annulée");
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new IllegalStateException("Une réservation terminée ne peut pas être annulée");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation complete(UUID id) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Seule une réservation CONFIRMED peut être complétée");
        }

        reservation.setStatus(ReservationStatus.COMPLETED);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation applyDiscount(UUID id, BigDecimal discountedUnitPrice, String reason, String pricedBySub) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Le prix ne peut être modifié que sur une réservation PENDING");
        }

        BigDecimal totalPrice = discountedUnitPrice.multiply(BigDecimal.valueOf(reservation.getNights()));

        reservation.setUnitPriceApplied(discountedUnitPrice);
        reservation.setTotalPrice(totalPrice);
        reservation.setPricingType(PricingType.DISCOUNT);
        reservation.setPricingReason(reason);
        reservation.setPricedBySub(pricedBySub);

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation applyFreeStay(UUID id, String reason, String pricedBySub) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Le prix ne peut être modifié que sur une réservation PENDING");
        }

        reservation.setUnitPriceApplied(BigDecimal.ZERO);
        reservation.setTotalPrice(BigDecimal.ZERO);
        reservation.setPricingType(PricingType.FREE);
        reservation.setPricingReason(reason);
        reservation.setPricedBySub(pricedBySub);

        return reservationRepository.save(reservation);
    }

    public boolean hasOverlap(UUID propertyId, LocalDate startDate, LocalDate endDate) {
        List<Reservation> overlapping = reservationRepository.findOverlappingReservations(propertyId, startDate, endDate);
        return !overlapping.isEmpty();
    }

    public boolean isTenant(UUID reservationId, String userSub) {
        Reservation reservation = findById(reservationId);
        return reservation.isTenant(userSub);
    }

    public boolean isPropertyOwner(UUID reservationId, String userSub) {
        Reservation reservation = findById(reservationId);
        return reservation.getProperty().isOwnedBy(userSub);
    }
}
