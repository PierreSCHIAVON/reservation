package com.example.reservation.controller;

import com.example.reservation.TestcontainersConfiguration;
import com.example.reservation.config.TestSecurityConfig;
import com.example.reservation.domain.property.Property;
import com.example.reservation.domain.property.PropertyStatus;
import com.example.reservation.domain.reservation.PricingType;
import com.example.reservation.domain.reservation.Reservation;
import com.example.reservation.domain.reservation.ReservationStatus;
import com.example.reservation.repository.PropertyRepository;
import com.example.reservation.repository.ReservationRepository;
import com.example.reservation.security.WithJwt;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurityConfig.class})
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private static final String OWNER_SUB = "owner-user-sub";
    private static final String TENANT_SUB = "tenant-user-sub";
    private static final String OTHER_USER_SUB = "other-user-sub";

    private Property testProperty;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        propertyRepository.deleteAll();

        testProperty = Property.builder()
                .ownerSub(OWNER_SUB)
                .title("Test Property")
                .description("A beautiful test property")
                .city("Paris")
                .pricePerNight(new BigDecimal("100.00"))
                .status(PropertyStatus.ACTIVE)
                .build();
        testProperty = propertyRepository.save(testProperty);
    }

    private Reservation createReservation(String tenantSub, ReservationStatus status, LocalDate startDate, LocalDate endDate) {
        long nights = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        Reservation reservation = Reservation.builder()
                .property(testProperty)
                .tenantSub(tenantSub)
                .startDate(startDate)
                .endDate(endDate)
                .status(status)
                .unitPriceApplied(testProperty.getPricePerNight())
                .totalPrice(testProperty.getPricePerNight().multiply(BigDecimal.valueOf(nights)))
                .pricingType(PricingType.NORMAL)
                .build();
        return reservationRepository.save(reservation);
    }

    // ===== GET /api/reservations/mine =====

    @Nested
    @DisplayName("GET /api/reservations/mine - Get my reservations as tenant")
    class GetMyReservations {

        @Test
        @WithJwt(subject = TENANT_SUB)
        @DisplayName("Returns reservations for authenticated tenant")
        void returnsReservationsForTenant() throws Exception {
            createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));
            createReservation(TENANT_SUB, ReservationStatus.CONFIRMED, LocalDate.now().plusDays(20), LocalDate.now().plusDays(25));
            createReservation(OTHER_USER_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(30), LocalDate.now().plusDays(35));

            mockMvc.perform(get("/api/reservations/mine"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements", is(2)));
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/reservations/mine"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== GET /api/reservations/owner =====

    @Nested
    @DisplayName("GET /api/reservations/owner - Get reservations for my properties")
    class GetReservationsForMyProperties {

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns reservations for owner's properties")
        void returnsReservationsForOwner() throws Exception {
            createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));
            createReservation(OTHER_USER_SUB, ReservationStatus.CONFIRMED, LocalDate.now().plusDays(20), LocalDate.now().plusDays(25));

            mockMvc.perform(get("/api/reservations/owner"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements", is(2)));
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/reservations/owner"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== GET /api/reservations/owner/pending =====

    @Nested
    @DisplayName("GET /api/reservations/owner/pending - Get pending reservations for my properties")
    class GetPendingReservations {

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns only pending reservations")
        void returnsOnlyPendingReservations() throws Exception {
            createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));
            createReservation(OTHER_USER_SUB, ReservationStatus.CONFIRMED, LocalDate.now().plusDays(20), LocalDate.now().plusDays(25));

            mockMvc.perform(get("/api/reservations/owner/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].status", is("PENDING")));
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/reservations/owner/pending"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== GET /api/reservations/{id} =====

    @Nested
    @DisplayName("GET /api/reservations/{id} - Get reservation by ID")
    class GetReservationById {

        @Test
        @WithJwt(subject = TENANT_SUB)
        @DisplayName("Returns reservation for tenant")
        void returnsReservationForTenant() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(get("/api/reservations/{id}", reservation.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(reservation.getId().toString())));
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns reservation for property owner")
        void returnsReservationForOwner() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(get("/api/reservations/{id}", reservation.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(reservation.getId().toString())));
        }

        @Test
        @WithJwt(subject = OTHER_USER_SUB)
        @DisplayName("Returns 403 for unauthorized user")
        void returns403ForUnauthorizedUser() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(get("/api/reservations/{id}", reservation.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(get("/api/reservations/{id}", reservation.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== POST /api/reservations =====

    @Nested
    @DisplayName("POST /api/reservations - Create reservation")
    class CreateReservation {

        @Test
        @WithJwt(subject = TENANT_SUB)
        @DisplayName("Creates reservation successfully")
        void createsReservationSuccessfully() throws Exception {
            Map<String, Object> request = Map.of(
                    "propertyId", testProperty.getId().toString(),
                    "startDate", LocalDate.now().plusDays(10).toString(),
                    "endDate", LocalDate.now().plusDays(15).toString()
            );

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.propertyId", is(testProperty.getId().toString())))
                    .andExpect(jsonPath("$.tenantSub", is(TENANT_SUB)))
                    .andExpect(jsonPath("$.status", is("PENDING")))
                    .andExpect(jsonPath("$.nights", is(5)))
                    .andExpect(jsonPath("$.totalPrice", is(500.00)));

            assertThat(reservationRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            Map<String, Object> request = Map.of(
                    "propertyId", testProperty.getId().toString(),
                    "startDate", LocalDate.now().plusDays(10).toString(),
                    "endDate", LocalDate.now().plusDays(15).toString()
            );

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithJwt(subject = TENANT_SUB)
        @DisplayName("Returns 400 for invalid dates - end before start")
        void returns400ForInvalidDates() throws Exception {
            Map<String, Object> request = Map.of(
                    "propertyId", testProperty.getId().toString(),
                    "startDate", LocalDate.now().plusDays(15).toString(),
                    "endDate", LocalDate.now().plusDays(10).toString()
            );

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithJwt(subject = TENANT_SUB)
        @DisplayName("Returns 409 for overlapping dates")
        void returns409ForOverlappingDates() throws Exception {
            createReservation(OTHER_USER_SUB, ReservationStatus.CONFIRMED, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            Map<String, Object> request = Map.of(
                    "propertyId", testProperty.getId().toString(),
                    "startDate", LocalDate.now().plusDays(12).toString(),
                    "endDate", LocalDate.now().plusDays(18).toString()
            );

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail", containsString("chevauchent")));
        }

        @Test
        @WithJwt(subject = TENANT_SUB)
        @DisplayName("Returns 409 for inactive property")
        void returns409ForInactiveProperty() throws Exception {
            testProperty.setStatus(PropertyStatus.INACTIVE);
            propertyRepository.save(testProperty);

            Map<String, Object> request = Map.of(
                    "propertyId", testProperty.getId().toString(),
                    "startDate", LocalDate.now().plusDays(10).toString(),
                    "endDate", LocalDate.now().plusDays(15).toString()
            );

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail", containsString("pas disponible")));
        }

        @Test
        @WithJwt(subject = TENANT_SUB)
        @DisplayName("Returns 404 for non-existent property")
        void returns404ForNonExistentProperty() throws Exception {
            Map<String, Object> request = Map.of(
                    "propertyId", UUID.randomUUID().toString(),
                    "startDate", LocalDate.now().plusDays(10).toString(),
                    "endDate", LocalDate.now().plusDays(15).toString()
            );

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    // ===== POST /api/reservations/{id}/confirm =====

    @Nested
    @DisplayName("POST /api/reservations/{id}/confirm - Confirm reservation")
    class ConfirmReservation {

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Confirms reservation successfully")
        void confirmsReservationSuccessfully() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(post("/api/reservations/{id}/confirm", reservation.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CONFIRMED")));
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns 409 when not pending")
        void returns409WhenNotPending() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.CONFIRMED, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(post("/api/reservations/{id}/confirm", reservation.getId()))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithJwt(subject = TENANT_SUB)
        @DisplayName("Returns 403 for tenant (only owner can confirm)")
        void returns403ForTenant() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(post("/api/reservations/{id}/confirm", reservation.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(post("/api/reservations/{id}/confirm", reservation.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== POST /api/reservations/{id}/cancel =====

    @Nested
    @DisplayName("POST /api/reservations/{id}/cancel - Cancel reservation")
    class CancelReservation {

        @Test
        @WithJwt(subject = TENANT_SUB)
        @DisplayName("Cancels reservation by tenant")
        void cancelsByTenant() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELLED")));
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Cancels reservation by owner")
        void cancelsByOwner() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.CONFIRMED, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELLED")));
        }

        @Test
        @WithJwt(subject = TENANT_SUB)
        @DisplayName("Returns 409 when already cancelled")
        void returns409WhenAlreadyCancelled() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.CANCELLED, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId()))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithJwt(subject = TENANT_SUB)
        @DisplayName("Returns 409 when completed")
        void returns409WhenCompleted() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.COMPLETED, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId()))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithJwt(subject = OTHER_USER_SUB)
        @DisplayName("Returns 403 for unauthorized user")
        void returns403ForUnauthorizedUser() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== POST /api/reservations/{id}/complete =====

    @Nested
    @DisplayName("POST /api/reservations/{id}/complete - Complete reservation")
    class CompleteReservation {

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Completes reservation successfully")
        void completesReservationSuccessfully() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.CONFIRMED, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(post("/api/reservations/{id}/complete", reservation.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("COMPLETED")));
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns 409 when not confirmed")
        void returns409WhenNotConfirmed() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(post("/api/reservations/{id}/complete", reservation.getId()))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithJwt(subject = TENANT_SUB)
        @DisplayName("Returns 403 for tenant")
        void returns403ForTenant() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.CONFIRMED, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(post("/api/reservations/{id}/complete", reservation.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.CONFIRMED, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(post("/api/reservations/{id}/complete", reservation.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== POST /api/reservations/{id}/discount =====

    @Nested
    @DisplayName("POST /api/reservations/{id}/discount - Apply discount")
    class ApplyDiscount {

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Applies discount successfully")
        void appliesDiscountSuccessfully() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            Map<String, Object> request = Map.of(
                    "discountedUnitPrice", 80.00,
                    "reason", "Fidèle client"
            );

            mockMvc.perform(post("/api/reservations/{id}/discount", reservation.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricingType", is("DISCOUNT")))
                    .andExpect(jsonPath("$.unitPriceApplied", is(80.00)))
                    .andExpect(jsonPath("$.totalPrice", is(400.00)))
                    .andExpect(jsonPath("$.pricingReason", is("Fidèle client")));
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns 409 when not pending")
        void returns409WhenNotPending() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.CONFIRMED, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            Map<String, Object> request = Map.of(
                    "discountedUnitPrice", 80.00,
                    "reason", "Fidèle client"
            );

            mockMvc.perform(post("/api/reservations/{id}/discount", reservation.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithJwt(subject = TENANT_SUB)
        @DisplayName("Returns 403 for tenant")
        void returns403ForTenant() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            Map<String, Object> request = Map.of(
                    "discountedUnitPrice", 80.00,
                    "reason", "Fidèle client"
            );

            mockMvc.perform(post("/api/reservations/{id}/discount", reservation.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            Map<String, Object> request = Map.of(
                    "discountedUnitPrice", 80.00,
                    "reason", "Fidèle client"
            );

            mockMvc.perform(post("/api/reservations/{id}/discount", reservation.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== POST /api/reservations/{id}/free =====

    @Nested
    @DisplayName("POST /api/reservations/{id}/free - Apply free stay")
    class ApplyFreeStay {

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Applies free stay successfully")
        void appliesFreeStaySuccessfully() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            Map<String, Object> request = Map.of("reason", "Compensation incident");

            mockMvc.perform(post("/api/reservations/{id}/free", reservation.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricingType", is("FREE")))
                    .andExpect(jsonPath("$.unitPriceApplied", is(0)))
                    .andExpect(jsonPath("$.totalPrice", is(0)))
                    .andExpect(jsonPath("$.pricingReason", is("Compensation incident")));
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns 409 when not pending")
        void returns409WhenNotPending() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.CONFIRMED, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            Map<String, Object> request = Map.of("reason", "Compensation incident");

            mockMvc.perform(post("/api/reservations/{id}/free", reservation.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns 400 without reason")
        void returns400WithoutReason() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            mockMvc.perform(post("/api/reservations/{id}/free", reservation.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithJwt(subject = TENANT_SUB)
        @DisplayName("Returns 403 for tenant")
        void returns403ForTenant() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            Map<String, Object> request = Map.of("reason", "Compensation incident");

            mockMvc.perform(post("/api/reservations/{id}/free", reservation.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            Reservation reservation = createReservation(TENANT_SUB, ReservationStatus.PENDING, LocalDate.now().plusDays(10), LocalDate.now().plusDays(15));

            Map<String, Object> request = Map.of("reason", "Compensation incident");

            mockMvc.perform(post("/api/reservations/{id}/free", reservation.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
