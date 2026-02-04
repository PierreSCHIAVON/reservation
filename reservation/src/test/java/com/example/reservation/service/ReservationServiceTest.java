package com.example.reservation.service;

import com.example.reservation.domain.property.Property;
import com.example.reservation.domain.property.PropertyStatus;
import com.example.reservation.domain.reservation.PricingType;
import com.example.reservation.domain.reservation.Reservation;
import com.example.reservation.domain.reservation.ReservationStatus;
import com.example.reservation.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
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
@DisplayName("ReservationService Unit Tests")
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PropertyService propertyService;

    @InjectMocks
    private ReservationService reservationService;

    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final UUID PROPERTY_ID = UUID.randomUUID();
    private static final String TENANT_SUB = "tenant-123";
    private static final String OWNER_SUB = "owner-456";
    private static final LocalDate START_DATE = LocalDate.of(2026, 3, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 3, 10);

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

    private Reservation createTestReservation(Property property, ReservationStatus status) {
        return Reservation.builder()
                .id(RESERVATION_ID)
                .property(property)
                .tenantSub(TENANT_SUB)
                .startDate(START_DATE)
                .endDate(END_DATE)
                .status(status)
                .unitPriceApplied(new BigDecimal("100.00"))
                .totalPrice(new BigDecimal("900.00"))
                .pricingType(PricingType.NORMAL)
                .build();
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Should return reservation with property when found")
        void shouldReturnReservationWhenFound() {
            Property property = createTestProperty();
            Reservation reservation = createTestReservation(property, ReservationStatus.PENDING);
            when(reservationRepository.findByIdWithProperty(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));

            Reservation result = reservationService.findById(RESERVATION_ID);

            assertThat(result).isEqualTo(reservation);
            assertThat(result.getProperty()).isNotNull();
            verify(reservationRepository).findByIdWithProperty(RESERVATION_ID);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(reservationRepository.findByIdWithProperty(RESERVATION_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.findById(RESERVATION_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Reservation not found");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Should create reservation with correct price calculation")
        void shouldCreateReservationWithCorrectPrice() {
            Property property = createTestProperty();
            when(propertyService.findById(PROPERTY_ID)).thenReturn(property);
            when(reservationRepository.findOverlappingReservations(any(), any(), any()))
                    .thenReturn(List.of());
            when(reservationRepository.save(any(Reservation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Reservation result = reservationService.create(
                    PROPERTY_ID, TENANT_SUB, START_DATE, END_DATE
            );

            ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
            verify(reservationRepository).save(captor.capture());

            Reservation saved = captor.getValue();
            assertThat(saved.getProperty()).isEqualTo(property);
            assertThat(saved.getTenantSub()).isEqualTo(TENANT_SUB);
            assertThat(saved.getStartDate()).isEqualTo(START_DATE);
            assertThat(saved.getEndDate()).isEqualTo(END_DATE);
            assertThat(saved.getStatus()).isEqualTo(ReservationStatus.PENDING);
            assertThat(saved.getUnitPriceApplied()).isEqualByComparingTo("100.00");
            // 9 nights = 900.00
            assertThat(saved.getTotalPrice()).isEqualByComparingTo("900.00");
            assertThat(saved.getPricingType()).isEqualTo(PricingType.NORMAL);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when property not bookable")
        void shouldThrowWhenPropertyNotBookable() {
            Property property = createTestProperty();
            property.setStatus(PropertyStatus.INACTIVE);
            when(propertyService.findById(PROPERTY_ID)).thenReturn(property);

            assertThatThrownBy(() -> reservationService.create(
                    PROPERTY_ID, TENANT_SUB, START_DATE, END_DATE
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("n'est pas disponible");

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw IllegalStateException when dates overlap")
        void shouldThrowWhenDatesOverlap() {
            Property property = createTestProperty();
            Reservation existing = createTestReservation(property, ReservationStatus.CONFIRMED);
            when(propertyService.findById(PROPERTY_ID)).thenReturn(property);
            when(reservationRepository.findOverlappingReservations(
                    PROPERTY_ID, START_DATE, END_DATE
            )).thenReturn(List.of(existing));

            assertThatThrownBy(() -> reservationService.create(
                    PROPERTY_ID, TENANT_SUB, START_DATE, END_DATE
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("chevauchent");

            verify(reservationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("Should confirm PENDING reservation")
        void shouldConfirmPendingReservation() {
            Property property = createTestProperty();
            Reservation reservation = createTestReservation(property, ReservationStatus.PENDING);
            when(reservationRepository.findByIdWithProperty(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any(Reservation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Reservation result = reservationService.confirm(RESERVATION_ID);

            assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            verify(reservationRepository).save(reservation);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when reservation not PENDING")
        void shouldThrowWhenNotPending() {
            Property property = createTestProperty();
            Reservation reservation = createTestReservation(property, ReservationStatus.CONFIRMED);
            when(reservationRepository.findByIdWithProperty(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.confirm(RESERVATION_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");

            verify(reservationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("Should cancel PENDING reservation")
        void shouldCancelPendingReservation() {
            Property property = createTestProperty();
            Reservation reservation = createTestReservation(property, ReservationStatus.PENDING);
            when(reservationRepository.findByIdWithProperty(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any(Reservation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Reservation result = reservationService.cancel(RESERVATION_ID);

            assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            verify(reservationRepository).save(reservation);
        }

        @Test
        @DisplayName("Should cancel CONFIRMED reservation")
        void shouldCancelConfirmedReservation() {
            Property property = createTestProperty();
            Reservation reservation = createTestReservation(property, ReservationStatus.CONFIRMED);
            when(reservationRepository.findByIdWithProperty(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any(Reservation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Reservation result = reservationService.cancel(RESERVATION_ID);

            assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should throw when already CANCELLED")
        void shouldThrowWhenAlreadyCancelled() {
            Property property = createTestProperty();
            Reservation reservation = createTestReservation(property, ReservationStatus.CANCELLED);
            when(reservationRepository.findByIdWithProperty(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.cancel(RESERVATION_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("déjà annulée");
        }

        @Test
        @DisplayName("Should throw when COMPLETED")
        void shouldThrowWhenCompleted() {
            Property property = createTestProperty();
            Reservation reservation = createTestReservation(property, ReservationStatus.COMPLETED);
            when(reservationRepository.findByIdWithProperty(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.cancel(RESERVATION_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("terminée");
        }
    }

    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("Should complete CONFIRMED reservation")
        void shouldCompleteConfirmedReservation() {
            Property property = createTestProperty();
            Reservation reservation = createTestReservation(property, ReservationStatus.CONFIRMED);
            when(reservationRepository.findByIdWithProperty(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any(Reservation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Reservation result = reservationService.complete(RESERVATION_ID);

            assertThat(result.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
            verify(reservationRepository).save(reservation);
        }

        @Test
        @DisplayName("Should throw when reservation not CONFIRMED")
        void shouldThrowWhenNotConfirmed() {
            Property property = createTestProperty();
            Reservation reservation = createTestReservation(property, ReservationStatus.PENDING);
            when(reservationRepository.findByIdWithProperty(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.complete(RESERVATION_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CONFIRMED");
        }
    }

    @Nested
    @DisplayName("applyDiscount")
    class ApplyDiscount {

        @Test
        @DisplayName("Should apply discount to PENDING reservation")
        void shouldApplyDiscountToPendingReservation() {
            Property property = createTestProperty();
            Reservation reservation = createTestReservation(property, ReservationStatus.PENDING);
            when(reservationRepository.findByIdWithProperty(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any(Reservation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            BigDecimal discountedPrice = new BigDecimal("80.00");
            Reservation result = reservationService.applyDiscount(
                    RESERVATION_ID, discountedPrice, "Test discount", OWNER_SUB
            );

            assertThat(result.getUnitPriceApplied()).isEqualByComparingTo("80.00");
            assertThat(result.getTotalPrice()).isEqualByComparingTo("720.00"); // 9 nights * 80
            assertThat(result.getPricingType()).isEqualTo(PricingType.DISCOUNT);
            assertThat(result.getPricingReason()).isEqualTo("Test discount");
            assertThat(result.getPricedBySub()).isEqualTo(OWNER_SUB);
        }

        @Test
        @DisplayName("Should allow 0 price discount")
        void shouldAllowZeroPriceDiscount() {
            Property property = createTestProperty();
            Reservation reservation = createTestReservation(property, ReservationStatus.PENDING);
            when(reservationRepository.findByIdWithProperty(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any(Reservation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Reservation result = reservationService.applyDiscount(
                    RESERVATION_ID, BigDecimal.ZERO, "Free stay", OWNER_SUB
            );

            assertThat(result.getUnitPriceApplied()).isEqualByComparingTo("0.00");
            assertThat(result.getTotalPrice()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("Should throw when discounted price exceeds original price")
        void shouldThrowWhenDiscountedPriceExceedsOriginal() {
            Property property = createTestProperty();
            Reservation reservation = createTestReservation(property, ReservationStatus.PENDING);
            when(reservationRepository.findByIdWithProperty(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));

            BigDecimal tooHighPrice = new BigDecimal("150.00");
            assertThatThrownBy(() -> reservationService.applyDiscount(
                    RESERVATION_ID, tooHighPrice, "Invalid", OWNER_SUB
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ne peut pas être supérieur");
        }

        @Test
        @DisplayName("Should throw when reservation not PENDING")
        void shouldThrowWhenNotPending() {
            Property property = createTestProperty();
            Reservation reservation = createTestReservation(property, ReservationStatus.CONFIRMED);
            when(reservationRepository.findByIdWithProperty(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.applyDiscount(
                    RESERVATION_ID, new BigDecimal("80.00"), "Test", OWNER_SUB
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }
    }

    @Nested
    @DisplayName("applyFreeStay")
    class ApplyFreeStay {

        @Test
        @DisplayName("Should apply free stay to PENDING reservation")
        void shouldApplyFreeStayToPendingReservation() {
            Property property = createTestProperty();
            Reservation reservation = createTestReservation(property, ReservationStatus.PENDING);
            when(reservationRepository.findByIdWithProperty(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));
            when(reservationRepository.save(any(Reservation.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Reservation result = reservationService.applyFreeStay(
                    RESERVATION_ID, "Promotional offer", OWNER_SUB
            );

            assertThat(result.getUnitPriceApplied()).isEqualByComparingTo("0.00");
            assertThat(result.getTotalPrice()).isEqualByComparingTo("0.00");
            assertThat(result.getPricingType()).isEqualTo(PricingType.FREE);
            assertThat(result.getPricingReason()).isEqualTo("Promotional offer");
            assertThat(result.getPricedBySub()).isEqualTo(OWNER_SUB);
        }

        @Test
        @DisplayName("Should throw when reservation not PENDING")
        void shouldThrowWhenNotPending() {
            Property property = createTestProperty();
            Reservation reservation = createTestReservation(property, ReservationStatus.CONFIRMED);
            when(reservationRepository.findByIdWithProperty(RESERVATION_ID))
                    .thenReturn(Optional.of(reservation));

            assertThatThrownBy(() -> reservationService.applyFreeStay(
                    RESERVATION_ID, "Test", OWNER_SUB
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }
    }

    @Nested
    @DisplayName("Query Methods")
    class QueryMethods {

        @Test
        @DisplayName("findByTenant should return tenant's reservations")
        void findByTenantShouldReturnTenantReservations() {
            List<Reservation> expected = List.of(
                    createTestReservation(createTestProperty(), ReservationStatus.PENDING)
            );
            when(reservationRepository.findByTenantSub(TENANT_SUB)).thenReturn(expected);

            List<Reservation> result = reservationService.findByTenant(TENANT_SUB);

            assertThat(result).hasSize(1);
            verify(reservationRepository).findByTenantSub(TENANT_SUB);
        }

        @Test
        @DisplayName("findByPropertyOwner should return owner's reservations")
        void findByPropertyOwnerShouldReturnOwnerReservations() {
            List<Reservation> expected = List.of(
                    createTestReservation(createTestProperty(), ReservationStatus.PENDING)
            );
            when(reservationRepository.findByPropertyOwnerSub(OWNER_SUB)).thenReturn(expected);

            List<Reservation> result = reservationService.findByPropertyOwner(OWNER_SUB);

            assertThat(result).hasSize(1);
            verify(reservationRepository).findByPropertyOwnerSub(OWNER_SUB);
        }

        @Test
        @DisplayName("hasOverlap should return true when overlap exists")
        void hasOverlapShouldReturnTrueWhenOverlapExists() {
            List<Reservation> overlapping = List.of(
                    createTestReservation(createTestProperty(), ReservationStatus.CONFIRMED)
            );
            when(reservationRepository.findOverlappingReservations(
                    PROPERTY_ID, START_DATE, END_DATE
            )).thenReturn(overlapping);

            boolean result = reservationService.hasOverlap(PROPERTY_ID, START_DATE, END_DATE);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("hasOverlap should return false when no overlap")
        void hasOverlapShouldReturnFalseWhenNoOverlap() {
            when(reservationRepository.findOverlappingReservations(
                    PROPERTY_ID, START_DATE, END_DATE
            )).thenReturn(List.of());

            boolean result = reservationService.hasOverlap(PROPERTY_ID, START_DATE, END_DATE);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Paginated Methods")
    class PaginatedMethods {

        @Test
        @DisplayName("findByTenant paginated should maintain order")
        void findByTenantPaginatedShouldMaintainOrder() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 20);
            Page<UUID> idsPage = new PageImpl<>(List.of(id1, id2), pageable, 2);

            Property property = createTestProperty();
            Reservation res1 = createTestReservation(property, ReservationStatus.PENDING);
            res1.setId(id1);
            Reservation res2 = createTestReservation(property, ReservationStatus.CONFIRMED);
            res2.setId(id2);

            when(reservationRepository.findIdsByTenantSub(TENANT_SUB, pageable))
                    .thenReturn(idsPage);
            when(reservationRepository.findByIdsWithProperty(List.of(id1, id2)))
                    .thenReturn(List.of(res2, res1)); // Intentionally reversed

            Page<Reservation> result = reservationService.findByTenant(TENANT_SUB, pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getId()).isEqualTo(id1);
            assertThat(result.getContent().get(1).getId()).isEqualTo(id2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }
    }
}
