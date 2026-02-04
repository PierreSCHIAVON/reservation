package com.example.reservation.service;

import com.example.reservation.domain.property.Property;
import com.example.reservation.domain.property.PropertyStatus;
import com.example.reservation.exception.InvalidStateException;
import com.example.reservation.exception.ResourceNotFoundException;
import com.example.reservation.repository.PropertyRepository;
import com.example.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final ReservationRepository reservationRepository;

    public Property findById(UUID id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property", id));
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

    // ===== Paginated methods =====

    public Page<Property> findByOwner(String ownerSub, Pageable pageable) {
        return propertyRepository.findByOwnerSub(ownerSub, pageable);
    }

    public Page<Property> findActiveProperties(Pageable pageable) {
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE, pageable);
    }

    public Page<Property> findActivePropertiesByCity(String city, Pageable pageable) {
        return propertyRepository.findByStatusAndCityIgnoreCase(PropertyStatus.ACTIVE, city, pageable);
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
            throw new InvalidStateException("La propriété est déjà active");
        }

        property.setStatus(PropertyStatus.ACTIVE);
        return propertyRepository.save(property);
    }

    @Transactional
    public Property deactivate(UUID id) {
        Property property = findById(id);

        if (property.getStatus() == PropertyStatus.INACTIVE) {
            throw new InvalidStateException("La propriété est déjà inactive");
        }

        property.setStatus(PropertyStatus.INACTIVE);
        return propertyRepository.save(property);
    }

    public boolean isOwner(UUID propertyId, String userSub) {
        return propertyRepository.existsByIdAndOwnerSub(propertyId, userSub);
    }

    public boolean hasOverlappingReservation(UUID propertyId, LocalDate startDate, LocalDate endDate) {
        return reservationRepository.existsOverlappingReservation(propertyId, startDate, endDate);
    }

    @Transactional
    public void delete(UUID id) {
        Property property = findById(id);
        propertyRepository.delete(property);
    }
}
