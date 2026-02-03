package com.example.reservation.repository;

import com.example.reservation.domain.reservation.Reservation;
import com.example.reservation.domain.reservation.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @Query("SELECT r FROM Reservation r JOIN FETCH r.property WHERE r.id = :id")
    Optional<Reservation> findByIdWithProperty(@Param("id") UUID id);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.property WHERE r.tenantSub = :tenantSub")
    List<Reservation> findByTenantSub(@Param("tenantSub") String tenantSub);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.property WHERE r.property.id = :propertyId")
    List<Reservation> findByPropertyId(@Param("propertyId") UUID propertyId);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.property WHERE r.status = :status")
    List<Reservation> findByStatus(@Param("status") ReservationStatus status);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.property WHERE r.tenantSub = :tenantSub AND r.status = :status")
    List<Reservation> findByTenantSubAndStatus(
            @Param("tenantSub") String tenantSub,
            @Param("status") ReservationStatus status
    );

    @Query("SELECT r FROM Reservation r JOIN FETCH r.property WHERE r.property.id = :propertyId AND r.status = :status")
    List<Reservation> findByPropertyIdAndStatus(
            @Param("propertyId") UUID propertyId,
            @Param("status") ReservationStatus status
    );

    @Query("SELECT r FROM Reservation r JOIN FETCH r.property WHERE r.property.id = :propertyId " +
           "AND r.status IN ('PENDING', 'CONFIRMED') " +
           "AND r.startDate <= :endDate AND r.endDate >= :startDate")
    List<Reservation> findOverlappingReservations(
            @Param("propertyId") UUID propertyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.property.id = :propertyId " +
           "AND r.status IN ('PENDING', 'CONFIRMED') " +
           "AND r.startDate <= :endDate AND r.endDate >= :startDate")
    boolean existsOverlappingReservation(
            @Param("propertyId") UUID propertyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT r FROM Reservation r JOIN FETCH r.property WHERE r.property.ownerSub = :ownerSub")
    List<Reservation> findByPropertyOwnerSub(@Param("ownerSub") String ownerSub);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.property WHERE r.property.ownerSub = :ownerSub AND r.status = :status")
    List<Reservation> findByPropertyOwnerSubAndStatus(
            @Param("ownerSub") String ownerSub,
            @Param("status") ReservationStatus status
    );

    // === Authorization queries (optimized for permission checks) ===

    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.id = :id AND r.tenantSub = :tenantSub")
    boolean existsByIdAndTenantSub(@Param("id") UUID id, @Param("tenantSub") String tenantSub);

    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.id = :id AND r.property.ownerSub = :ownerSub")
    boolean existsByIdAndPropertyOwnerSub(@Param("id") UUID id, @Param("ownerSub") String ownerSub);

    // === Paginated queries (two-query pattern for JOIN FETCH compatibility) ===

    // Step 1: Get paginated IDs
    @Query("SELECT r.id FROM Reservation r WHERE r.tenantSub = :tenantSub")
    Page<UUID> findIdsByTenantSub(@Param("tenantSub") String tenantSub, Pageable pageable);

    @Query("SELECT r.id FROM Reservation r WHERE r.property.ownerSub = :ownerSub")
    Page<UUID> findIdsByPropertyOwnerSub(@Param("ownerSub") String ownerSub, Pageable pageable);

    @Query("SELECT r.id FROM Reservation r WHERE r.property.ownerSub = :ownerSub AND r.status = :status")
    Page<UUID> findIdsByPropertyOwnerSubAndStatus(
            @Param("ownerSub") String ownerSub,
            @Param("status") ReservationStatus status,
            Pageable pageable
    );

    // Step 2: Fetch entities with JOIN FETCH by IDs
    @Query("SELECT r FROM Reservation r JOIN FETCH r.property WHERE r.id IN :ids")
    List<Reservation> findByIdsWithProperty(@Param("ids") List<UUID> ids);
}
