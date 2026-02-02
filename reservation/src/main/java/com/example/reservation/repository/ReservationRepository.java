package com.example.reservation.repository;

import com.example.reservation.domain.reservation.Reservation;
import com.example.reservation.domain.reservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByTenantSub(String tenantSub);

    List<Reservation> findByPropertyId(UUID propertyId);

    List<Reservation> findByStatus(ReservationStatus status);

    List<Reservation> findByTenantSubAndStatus(String tenantSub, ReservationStatus status);

    List<Reservation> findByPropertyIdAndStatus(UUID propertyId, ReservationStatus status);

    @Query("SELECT r FROM Reservation r WHERE r.property.id = :propertyId " +
           "AND r.status IN ('PENDING', 'CONFIRMED') " +
           "AND r.startDate <= :endDate AND r.endDate >= :startDate")
    List<Reservation> findOverlappingReservations(
            @Param("propertyId") UUID propertyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT r FROM Reservation r WHERE r.property.ownerSub = :ownerSub")
    List<Reservation> findByPropertyOwnerSub(@Param("ownerSub") String ownerSub);

    @Query("SELECT r FROM Reservation r WHERE r.property.ownerSub = :ownerSub AND r.status = :status")
    List<Reservation> findByPropertyOwnerSubAndStatus(
            @Param("ownerSub") String ownerSub,
            @Param("status") ReservationStatus status
    );
}
