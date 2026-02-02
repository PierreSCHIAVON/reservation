package com.example.reservation.service;

import com.example.reservation.domain.property.Property;
import com.example.reservation.domain.property.PropertyStatus;
import com.example.reservation.repository.PropertyRepository;
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
public class PropertyService {

    private final PropertyRepository propertyRepository;

    public Property findById(UUID id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Property not found: " + id));
    }

    public List<Property> findByOwner(String ownerSub) {
        return propertyRepository.findByOwnerSub(ownerSub);
    }

    public List<Property> findActiveProperties() {
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE);
    }

    public List<Property> findActivePropertiesByCity(String city) {
        return propertyRepository.findByStatusAndCityIgnoreCase(PropertyStatus.ACTIVE, city);
    }

    @Transactional
    public Property create(String ownerSub, String title, String description, String city, BigDecimal pricePerNight) {
        Property property = Property.builder()
                .ownerSub(ownerSub)
                .title(title)
                .description(description)
                .city(city)
                .pricePerNight(pricePerNight)
                .status(PropertyStatus.ACTIVE)
                .build();

        return propertyRepository.save(property);
    }

    @Transactional
    public Property update(UUID id, String title, String description, String city, BigDecimal pricePerNight) {
        Property property = findById(id);

        if (title != null) {
            property.setTitle(title);
        }
        if (description != null) {
            property.setDescription(description);
        }
        if (city != null) {
            property.setCity(city);
        }
        if (pricePerNight != null) {
            property.setPricePerNight(pricePerNight);
        }

        return propertyRepository.save(property);
    }

    @Transactional
    public Property activate(UUID id) {
        Property property = findById(id);

        if (property.getStatus() == PropertyStatus.ACTIVE) {
            throw new IllegalStateException("La propriété est déjà active");
        }

        property.setStatus(PropertyStatus.ACTIVE);
        return propertyRepository.save(property);
    }

    @Transactional
    public Property deactivate(UUID id) {
        Property property = findById(id);

        if (property.getStatus() == PropertyStatus.INACTIVE) {
            throw new IllegalStateException("La propriété est déjà inactive");
        }

        property.setStatus(PropertyStatus.INACTIVE);
        return propertyRepository.save(property);
    }

    public boolean isOwner(UUID propertyId, String userSub) {
        return propertyRepository.existsByIdAndOwnerSub(propertyId, userSub);
    }

    public boolean hasOverlappingReservation(UUID propertyId, LocalDate startDate, LocalDate endDate) {
        Property property = findById(propertyId);
        return property.hasOverlap(startDate, endDate);
    }

    @Transactional
    public void delete(UUID id) {
        Property property = findById(id);
        propertyRepository.delete(property);
    }
}
