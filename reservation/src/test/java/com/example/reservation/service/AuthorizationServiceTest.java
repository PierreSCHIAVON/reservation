package com.example.reservation.service;

import com.example.reservation.repository.PropertyAccessCodeRepository;
import com.example.reservation.repository.PropertyRepository;
import com.example.reservation.repository.ReservationRepository;
import com.example.reservation.security.AuthorizationService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationService Unit Tests")
class AuthorizationServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PropertyAccessCodeRepository accessCodeRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    private static final UUID PROPERTY_ID = UUID.randomUUID();
    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final UUID ACCESS_CODE_ID = UUID.randomUUID();
    private static final String USER_SUB = "user-123";
    private static final String OTHER_USER_SUB = "user-456";

    @Nested
    @DisplayName("isPropertyOwner")
    class IsPropertyOwner {

        @Test
        @DisplayName("Should return true when user is property owner")
        void shouldReturnTrueWhenUserIsPropertyOwner() {
            when(propertyRepository.existsById(PROPERTY_ID)).thenReturn(true);
            when(propertyRepository.existsByIdAndOwnerSub(PROPERTY_ID, USER_SUB)).thenReturn(true);

            boolean result = authorizationService.isPropertyOwner(PROPERTY_ID, USER_SUB);

            assertThat(result).isTrue();
            verify(propertyRepository).existsById(PROPERTY_ID);
            verify(propertyRepository).existsByIdAndOwnerSub(PROPERTY_ID, USER_SUB);
        }

        @Test
        @DisplayName("Should return false when user is not property owner")
        void shouldReturnFalseWhenUserIsNotPropertyOwner() {
            when(propertyRepository.existsById(PROPERTY_ID)).thenReturn(true);
            when(propertyRepository.existsByIdAndOwnerSub(PROPERTY_ID, OTHER_USER_SUB)).thenReturn(false);

            boolean result = authorizationService.isPropertyOwner(PROPERTY_ID, OTHER_USER_SUB);

            assertThat(result).isFalse();
            verify(propertyRepository).existsById(PROPERTY_ID);
            verify(propertyRepository).existsByIdAndOwnerSub(PROPERTY_ID, OTHER_USER_SUB);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when property does not exist")
        void shouldThrowWhenPropertyDoesNotExist() {
            when(propertyRepository.existsById(PROPERTY_ID)).thenReturn(false);

            assertThatThrownBy(() -> authorizationService.isPropertyOwner(PROPERTY_ID, USER_SUB))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Property not found");

            verify(propertyRepository).existsById(PROPERTY_ID);
            verify(propertyRepository, never()).existsByIdAndOwnerSub(any(), any());
        }
    }

    @Nested
    @DisplayName("isReservationTenant")
    class IsReservationTenant {

        @Test
        @DisplayName("Should return true when user is tenant")
        void shouldReturnTrueWhenUserIsTenant() {
            when(reservationRepository.existsById(RESERVATION_ID)).thenReturn(true);
            when(reservationRepository.existsByIdAndTenantSub(RESERVATION_ID, USER_SUB)).thenReturn(true);

            boolean result = authorizationService.isReservationTenant(RESERVATION_ID, USER_SUB);

            assertThat(result).isTrue();
            verify(reservationRepository).existsById(RESERVATION_ID);
            verify(reservationRepository).existsByIdAndTenantSub(RESERVATION_ID, USER_SUB);
        }

        @Test
        @DisplayName("Should return false when user is not tenant")
        void shouldReturnFalseWhenUserIsNotTenant() {
            when(reservationRepository.existsById(RESERVATION_ID)).thenReturn(true);
            when(reservationRepository.existsByIdAndTenantSub(RESERVATION_ID, OTHER_USER_SUB)).thenReturn(false);

            boolean result = authorizationService.isReservationTenant(RESERVATION_ID, OTHER_USER_SUB);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when reservation does not exist")
        void shouldThrowWhenReservationDoesNotExist() {
            when(reservationRepository.existsById(RESERVATION_ID)).thenReturn(false);

            assertThatThrownBy(() -> authorizationService.isReservationTenant(RESERVATION_ID, USER_SUB))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Reservation not found");

            verify(reservationRepository).existsById(RESERVATION_ID);
            verify(reservationRepository, never()).existsByIdAndTenantSub(any(), any());
        }
    }

    @Nested
    @DisplayName("isReservationPropertyOwner")
    class IsReservationPropertyOwner {

        @Test
        @DisplayName("Should return true when user is property owner of reservation")
        void shouldReturnTrueWhenUserIsPropertyOwner() {
            when(reservationRepository.existsById(RESERVATION_ID)).thenReturn(true);
            when(reservationRepository.existsByIdAndPropertyOwnerSub(RESERVATION_ID, USER_SUB)).thenReturn(true);

            boolean result = authorizationService.isReservationPropertyOwner(RESERVATION_ID, USER_SUB);

            assertThat(result).isTrue();
            verify(reservationRepository).existsById(RESERVATION_ID);
            verify(reservationRepository).existsByIdAndPropertyOwnerSub(RESERVATION_ID, USER_SUB);
        }

        @Test
        @DisplayName("Should return false when user is not property owner")
        void shouldReturnFalseWhenUserIsNotPropertyOwner() {
            when(reservationRepository.existsById(RESERVATION_ID)).thenReturn(true);
            when(reservationRepository.existsByIdAndPropertyOwnerSub(RESERVATION_ID, OTHER_USER_SUB)).thenReturn(false);

            boolean result = authorizationService.isReservationPropertyOwner(RESERVATION_ID, OTHER_USER_SUB);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when reservation does not exist")
        void shouldThrowWhenReservationDoesNotExist() {
            when(reservationRepository.existsById(RESERVATION_ID)).thenReturn(false);

            assertThatThrownBy(() -> authorizationService.isReservationPropertyOwner(RESERVATION_ID, USER_SUB))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Reservation not found");

            verify(reservationRepository).existsById(RESERVATION_ID);
            verify(reservationRepository, never()).existsByIdAndPropertyOwnerSub(any(), any());
        }
    }

    @Nested
    @DisplayName("canAccessReservation")
    class CanAccessReservation {

        @Test
        @DisplayName("Should return true when user is tenant")
        void shouldReturnTrueWhenUserIsTenant() {
            when(reservationRepository.existsById(RESERVATION_ID)).thenReturn(true);
            when(reservationRepository.existsByIdAndTenantSub(RESERVATION_ID, USER_SUB)).thenReturn(true);

            boolean result = authorizationService.canAccessReservation(RESERVATION_ID, USER_SUB);

            assertThat(result).isTrue();
            verify(reservationRepository).existsByIdAndTenantSub(RESERVATION_ID, USER_SUB);
        }

        @Test
        @DisplayName("Should return true when user is property owner")
        void shouldReturnTrueWhenUserIsPropertyOwner() {
            when(reservationRepository.existsById(RESERVATION_ID)).thenReturn(true);
            when(reservationRepository.existsByIdAndTenantSub(RESERVATION_ID, USER_SUB)).thenReturn(false);
            when(reservationRepository.existsByIdAndPropertyOwnerSub(RESERVATION_ID, USER_SUB)).thenReturn(true);

            boolean result = authorizationService.canAccessReservation(RESERVATION_ID, USER_SUB);

            assertThat(result).isTrue();
            verify(reservationRepository).existsByIdAndTenantSub(RESERVATION_ID, USER_SUB);
            verify(reservationRepository).existsByIdAndPropertyOwnerSub(RESERVATION_ID, USER_SUB);
        }

        @Test
        @DisplayName("Should return false when user is neither tenant nor property owner")
        void shouldReturnFalseWhenUserIsNeither() {
            when(reservationRepository.existsById(RESERVATION_ID)).thenReturn(true);
            when(reservationRepository.existsByIdAndTenantSub(RESERVATION_ID, OTHER_USER_SUB)).thenReturn(false);
            when(reservationRepository.existsByIdAndPropertyOwnerSub(RESERVATION_ID, OTHER_USER_SUB)).thenReturn(false);

            boolean result = authorizationService.canAccessReservation(RESERVATION_ID, OTHER_USER_SUB);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when reservation does not exist")
        void shouldThrowWhenReservationDoesNotExist() {
            when(reservationRepository.existsById(RESERVATION_ID)).thenReturn(false);

            assertThatThrownBy(() -> authorizationService.canAccessReservation(RESERVATION_ID, USER_SUB))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Reservation not found");

            verify(reservationRepository).existsById(RESERVATION_ID);
            verify(reservationRepository, never()).existsByIdAndTenantSub(any(), any());
            verify(reservationRepository, never()).existsByIdAndPropertyOwnerSub(any(), any());
        }
    }

    @Nested
    @DisplayName("isAccessCodeCreator")
    class IsAccessCodeCreator {

        @Test
        @DisplayName("Should return true when user is access code creator")
        void shouldReturnTrueWhenUserIsCreator() {
            when(accessCodeRepository.existsById(ACCESS_CODE_ID)).thenReturn(true);
            when(accessCodeRepository.existsByIdAndCreatedBySub(ACCESS_CODE_ID, USER_SUB)).thenReturn(true);

            boolean result = authorizationService.isAccessCodeCreator(ACCESS_CODE_ID, USER_SUB);

            assertThat(result).isTrue();
            verify(accessCodeRepository).existsById(ACCESS_CODE_ID);
            verify(accessCodeRepository).existsByIdAndCreatedBySub(ACCESS_CODE_ID, USER_SUB);
        }

        @Test
        @DisplayName("Should return false when user is not creator")
        void shouldReturnFalseWhenUserIsNotCreator() {
            when(accessCodeRepository.existsById(ACCESS_CODE_ID)).thenReturn(true);
            when(accessCodeRepository.existsByIdAndCreatedBySub(ACCESS_CODE_ID, OTHER_USER_SUB)).thenReturn(false);

            boolean result = authorizationService.isAccessCodeCreator(ACCESS_CODE_ID, OTHER_USER_SUB);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when access code does not exist")
        void shouldThrowWhenAccessCodeDoesNotExist() {
            when(accessCodeRepository.existsById(ACCESS_CODE_ID)).thenReturn(false);

            assertThatThrownBy(() -> authorizationService.isAccessCodeCreator(ACCESS_CODE_ID, USER_SUB))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Access code not found");

            verify(accessCodeRepository).existsById(ACCESS_CODE_ID);
            verify(accessCodeRepository, never()).existsByIdAndCreatedBySub(any(), any());
        }
    }
}
