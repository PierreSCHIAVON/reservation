package com.example.reservation.service;

import com.example.reservation.domain.property.Property;
import com.example.reservation.domain.property.PropertyStatus;
import com.example.reservation.exception.InvalidStateException;
import com.example.reservation.exception.ResourceNotFoundException;
import com.example.reservation.repository.PropertyRepository;
import com.example.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PropertyService Unit Tests")
class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private PropertyService propertyService;

    private static final UUID PROPERTY_ID = UUID.randomUUID();
    private static final String OWNER_SUB = "owner-123";
    private static final String OTHER_USER_SUB = "other-456";

    private Property createTestProperty() {
        return Property.builder()
                .id(PROPERTY_ID)
                .ownerSub(OWNER_SUB)
                .title("Test Property")
                .description("Test Description")
                .city("Paris")
                .pricePerNight(new BigDecimal("100.00"))
                .status(PropertyStatus.ACTIVE)
                .build();
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Should return property when found")
        void shouldReturnPropertyWhenFound() {
            Property property = createTestProperty();
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

            Property result = propertyService.findById(PROPERTY_ID);

            assertThat(result).isEqualTo(property);
            verify(propertyRepository).findById(PROPERTY_ID);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when property not found")
        void shouldThrowWhenPropertyNotFound() {
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> propertyService.findById(PROPERTY_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Property not found");

            verify(propertyRepository).findById(PROPERTY_ID);
        }
    }

    @Nested
    @DisplayName("findByOwner")
    class FindByOwner {

        @Test
        @DisplayName("Should return properties owned by user")
        void shouldReturnPropertiesOwnedByUser() {
            List<Property> properties = List.of(createTestProperty());
            when(propertyRepository.findByOwnerSub(OWNER_SUB)).thenReturn(properties);

            List<Property> result = propertyService.findByOwner(OWNER_SUB);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOwnerSub()).isEqualTo(OWNER_SUB);
            verify(propertyRepository).findByOwnerSub(OWNER_SUB);
        }

        @Test
        @DisplayName("Should return paginated properties owned by user")
        void shouldReturnPaginatedPropertiesOwnedByUser() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Property> page = new PageImpl<>(List.of(createTestProperty()));
            when(propertyRepository.findByOwnerSub(OWNER_SUB, pageable)).thenReturn(page);

            Page<Property> result = propertyService.findByOwner(OWNER_SUB, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(propertyRepository).findByOwnerSub(OWNER_SUB, pageable);
        }
    }

    @Nested
    @DisplayName("findActiveProperties")
    class FindActiveProperties {

        @Test
        @DisplayName("Should return only active properties")
        void shouldReturnOnlyActiveProperties() {
            List<Property> properties = List.of(createTestProperty());
            when(propertyRepository.findByStatus(PropertyStatus.ACTIVE)).thenReturn(properties);

            List<Property> result = propertyService.findActiveProperties();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(PropertyStatus.ACTIVE);
            verify(propertyRepository).findByStatus(PropertyStatus.ACTIVE);
        }

        @Test
        @DisplayName("Should return paginated active properties")
        void shouldReturnPaginatedActiveProperties() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Property> page = new PageImpl<>(List.of(createTestProperty()));
            when(propertyRepository.findByStatus(PropertyStatus.ACTIVE, pageable)).thenReturn(page);

            Page<Property> result = propertyService.findActiveProperties(pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(propertyRepository).findByStatus(PropertyStatus.ACTIVE, pageable);
        }
    }

    @Nested
    @DisplayName("findActivePropertiesByCity")
    class FindActivePropertiesByCity {

        @Test
        @DisplayName("Should return active properties filtered by city")
        void shouldReturnActivePropertiesFilteredByCity() {
            List<Property> properties = List.of(createTestProperty());
            when(propertyRepository.findByStatusAndCityIgnoreCase(PropertyStatus.ACTIVE, "Paris"))
                    .thenReturn(properties);

            List<Property> result = propertyService.findActivePropertiesByCity("Paris");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCity()).isEqualTo("Paris");
            verify(propertyRepository).findByStatusAndCityIgnoreCase(PropertyStatus.ACTIVE, "Paris");
        }

        @Test
        @DisplayName("Should return paginated active properties filtered by city")
        void shouldReturnPaginatedActivePropertiesFilteredByCity() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Property> page = new PageImpl<>(List.of(createTestProperty()));
            when(propertyRepository.findByStatusAndCityIgnoreCase(PropertyStatus.ACTIVE, "Paris", pageable))
                    .thenReturn(page);

            Page<Property> result = propertyService.findActivePropertiesByCity("Paris", pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(propertyRepository).findByStatusAndCityIgnoreCase(PropertyStatus.ACTIVE, "Paris", pageable);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Should create property with ACTIVE status")
        void shouldCreatePropertyWithActiveStatus() {
            Property expectedProperty = createTestProperty();
            when(propertyRepository.save(any(Property.class))).thenReturn(expectedProperty);

            Property result = propertyService.create(
                    OWNER_SUB,
                    "Test Property",
                    "Test Description",
                    "Paris",
                    new BigDecimal("100.00")
            );

            ArgumentCaptor<Property> propertyCaptor = ArgumentCaptor.forClass(Property.class);
            verify(propertyRepository).save(propertyCaptor.capture());

            Property savedProperty = propertyCaptor.getValue();
            assertThat(savedProperty.getOwnerSub()).isEqualTo(OWNER_SUB);
            assertThat(savedProperty.getTitle()).isEqualTo("Test Property");
            assertThat(savedProperty.getDescription()).isEqualTo("Test Description");
            assertThat(savedProperty.getCity()).isEqualTo("Paris");
            assertThat(savedProperty.getPricePerNight()).isEqualByComparingTo("100.00");
            assertThat(savedProperty.getStatus()).isEqualTo(PropertyStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Should update all fields when provided")
        void shouldUpdateAllFieldsWhenProvided() {
            Property property = createTestProperty();
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(propertyRepository.save(any(Property.class))).thenReturn(property);

            propertyService.update(
                    PROPERTY_ID,
                    "Updated Title",
                    "Updated Description",
                    "Lyon",
                    new BigDecimal("150.00")
            );

            assertThat(property.getTitle()).isEqualTo("Updated Title");
            assertThat(property.getDescription()).isEqualTo("Updated Description");
            assertThat(property.getCity()).isEqualTo("Lyon");
            assertThat(property.getPricePerNight()).isEqualByComparingTo("150.00");
            verify(propertyRepository).save(property);
        }

        @Test
        @DisplayName("Should keep existing values when fields are null")
        void shouldKeepExistingValuesWhenFieldsAreNull() {
            Property property = createTestProperty();
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(propertyRepository.save(any(Property.class))).thenReturn(property);

            propertyService.update(PROPERTY_ID, null, null, null, null);

            assertThat(property.getTitle()).isEqualTo("Test Property");
            assertThat(property.getDescription()).isEqualTo("Test Description");
            assertThat(property.getCity()).isEqualTo("Paris");
            assertThat(property.getPricePerNight()).isEqualByComparingTo("100.00");
            verify(propertyRepository).save(property);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when property not found")
        void shouldThrowWhenPropertyNotFound() {
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> propertyService.update(
                    PROPERTY_ID, "Title", "Desc", "City", BigDecimal.TEN
            ))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(propertyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("activate")
    class Activate {

        @Test
        @DisplayName("Should activate inactive property")
        void shouldActivateInactiveProperty() {
            Property property = createTestProperty();
            property.setStatus(PropertyStatus.INACTIVE);
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(propertyRepository.save(any(Property.class))).thenReturn(property);

            Property result = propertyService.activate(PROPERTY_ID);

            assertThat(result.getStatus()).isEqualTo(PropertyStatus.ACTIVE);
            verify(propertyRepository).save(property);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when property already active")
        void shouldThrowWhenPropertyAlreadyActive() {
            Property property = createTestProperty();
            property.setStatus(PropertyStatus.ACTIVE);
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

            assertThatThrownBy(() -> propertyService.activate(PROPERTY_ID))
                    .isInstanceOf(InvalidStateException.class)
                    .hasMessageContaining("déjà active");

            verify(propertyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deactivate")
    class Deactivate {

        @Test
        @DisplayName("Should deactivate active property")
        void shouldDeactivateActiveProperty() {
            Property property = createTestProperty();
            property.setStatus(PropertyStatus.ACTIVE);
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));
            when(propertyRepository.save(any(Property.class))).thenReturn(property);

            Property result = propertyService.deactivate(PROPERTY_ID);

            assertThat(result.getStatus()).isEqualTo(PropertyStatus.INACTIVE);
            verify(propertyRepository).save(property);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when property already inactive")
        void shouldThrowWhenPropertyAlreadyInactive() {
            Property property = createTestProperty();
            property.setStatus(PropertyStatus.INACTIVE);
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

            assertThatThrownBy(() -> propertyService.deactivate(PROPERTY_ID))
                    .isInstanceOf(InvalidStateException.class)
                    .hasMessageContaining("déjà inactive");

            verify(propertyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("isOwner")
    class IsOwner {

        @Test
        @DisplayName("Should return true when user is owner")
        void shouldReturnTrueWhenUserIsOwner() {
            when(propertyRepository.existsByIdAndOwnerSub(PROPERTY_ID, OWNER_SUB)).thenReturn(true);

            boolean result = propertyService.isOwner(PROPERTY_ID, OWNER_SUB);

            assertThat(result).isTrue();
            verify(propertyRepository).existsByIdAndOwnerSub(PROPERTY_ID, OWNER_SUB);
        }

        @Test
        @DisplayName("Should return false when user is not owner")
        void shouldReturnFalseWhenUserIsNotOwner() {
            when(propertyRepository.existsByIdAndOwnerSub(PROPERTY_ID, OTHER_USER_SUB)).thenReturn(false);

            boolean result = propertyService.isOwner(PROPERTY_ID, OTHER_USER_SUB);

            assertThat(result).isFalse();
            verify(propertyRepository).existsByIdAndOwnerSub(PROPERTY_ID, OTHER_USER_SUB);
        }
    }

    @Nested
    @DisplayName("hasOverlappingReservation")
    class HasOverlappingReservation {

        @Test
        @DisplayName("Should return true when overlapping reservation exists")
        void shouldReturnTrueWhenOverlappingExists() {
            LocalDate start = LocalDate.of(2026, 3, 1);
            LocalDate end = LocalDate.of(2026, 3, 10);
            when(reservationRepository.existsOverlappingReservation(PROPERTY_ID, start, end))
                    .thenReturn(true);

            boolean result = propertyService.hasOverlappingReservation(PROPERTY_ID, start, end);

            assertThat(result).isTrue();
            verify(reservationRepository).existsOverlappingReservation(PROPERTY_ID, start, end);
        }

        @Test
        @DisplayName("Should return false when no overlapping reservation")
        void shouldReturnFalseWhenNoOverlapping() {
            LocalDate start = LocalDate.of(2026, 3, 1);
            LocalDate end = LocalDate.of(2026, 3, 10);
            when(reservationRepository.existsOverlappingReservation(PROPERTY_ID, start, end))
                    .thenReturn(false);

            boolean result = propertyService.hasOverlappingReservation(PROPERTY_ID, start, end);

            assertThat(result).isFalse();
            verify(reservationRepository).existsOverlappingReservation(PROPERTY_ID, start, end);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("Should delete property when found")
        void shouldDeletePropertyWhenFound() {
            Property property = createTestProperty();
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.of(property));

            propertyService.delete(PROPERTY_ID);

            verify(propertyRepository).delete(property);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when property not found")
        void shouldThrowWhenPropertyNotFound() {
            when(propertyRepository.findById(PROPERTY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> propertyService.delete(PROPERTY_ID))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(propertyRepository, never()).delete(any());
        }
    }
}
