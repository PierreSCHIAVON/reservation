package com.example.reservation.repository;

import com.example.reservation.domain.property.Property;
import com.example.reservation.domain.property.PropertyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {

    List<Property> findByOwnerSub(String ownerSub);

    List<Property> findByStatus(PropertyStatus status);

    List<Property> findByOwnerSubAndStatus(String ownerSub, PropertyStatus status);

    List<Property> findByCityIgnoreCase(String city);

    List<Property> findByStatusAndCityIgnoreCase(PropertyStatus status, String city);

    boolean existsByIdAndOwnerSub(UUID id, String ownerSub);
}
