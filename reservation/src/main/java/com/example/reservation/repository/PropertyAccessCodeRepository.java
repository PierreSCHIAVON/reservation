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

    Optional<PropertyAccessCode> findByCodeLookup(String codeLookup);

    List<PropertyAccessCode> findByPropertyId(UUID propertyId);

    List<PropertyAccessCode> findByIssuedToEmailIgnoreCase(String email);

    List<PropertyAccessCode> findByCreatedBySub(String createdBySub);

    @Query("SELECT pac FROM PropertyAccessCode pac WHERE pac.property.id = :propertyId " +
           "AND pac.revokedAt IS NULL AND pac.redeemedAt IS NULL " +
           "AND (pac.expiresAt IS NULL OR pac.expiresAt > CURRENT_TIMESTAMP)")
    List<PropertyAccessCode> findActiveByPropertyId(@Param("propertyId") UUID propertyId);

    @Query("SELECT pac FROM PropertyAccessCode pac WHERE pac.issuedToEmail = LOWER(:email) " +
           "AND pac.revokedAt IS NULL AND pac.redeemedAt IS NULL " +
           "AND (pac.expiresAt IS NULL OR pac.expiresAt > CURRENT_TIMESTAMP)")
    List<PropertyAccessCode> findActiveByEmail(@Param("email") String email);
}
