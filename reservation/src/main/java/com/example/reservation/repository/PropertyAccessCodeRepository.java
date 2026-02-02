package com.example.reservation.repository;

import com.example.reservation.domain.property.PropertyAccessCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyAccessCodeRepository extends JpaRepository<PropertyAccessCode, UUID> {

    @Query("SELECT pac FROM PropertyAccessCode pac JOIN FETCH pac.property WHERE pac.codeLookup = :codeLookup")
    Optional<PropertyAccessCode> findByCodeLookup(@Param("codeLookup") String codeLookup);

    @Query("SELECT pac FROM PropertyAccessCode pac JOIN FETCH pac.property WHERE pac.property.id = :propertyId")
    List<PropertyAccessCode> findByPropertyId(@Param("propertyId") UUID propertyId);

    @Query("SELECT pac FROM PropertyAccessCode pac JOIN FETCH pac.property WHERE LOWER(pac.issuedToEmail) = LOWER(:email)")
    List<PropertyAccessCode> findByIssuedToEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT pac FROM PropertyAccessCode pac JOIN FETCH pac.property WHERE pac.createdBySub = :createdBySub")
    List<PropertyAccessCode> findByCreatedBySub(@Param("createdBySub") String createdBySub);

    @Query("SELECT pac FROM PropertyAccessCode pac JOIN FETCH pac.property WHERE pac.property.id = :propertyId " +
           "AND pac.revokedAt IS NULL AND pac.redeemedAt IS NULL " +
           "AND (pac.expiresAt IS NULL OR pac.expiresAt > CURRENT_TIMESTAMP)")
    List<PropertyAccessCode> findActiveByPropertyId(@Param("propertyId") UUID propertyId);

    @Query("SELECT pac FROM PropertyAccessCode pac JOIN FETCH pac.property WHERE LOWER(pac.issuedToEmail) = LOWER(:email) " +
           "AND pac.revokedAt IS NULL AND pac.redeemedAt IS NULL " +
           "AND (pac.expiresAt IS NULL OR pac.expiresAt > CURRENT_TIMESTAMP)")
    List<PropertyAccessCode> findActiveByEmail(@Param("email") String email);

    // === Authorization queries (optimized for permission checks) ===

    @Query("SELECT COUNT(pac) > 0 FROM PropertyAccessCode pac WHERE pac.id = :id AND pac.createdBySub = :createdBySub")
    boolean existsByIdAndCreatedBySub(@Param("id") UUID id, @Param("createdBySub") String createdBySub);
}
