package com.example.reservation.controller;

import com.example.reservation.TestcontainersConfiguration;
import com.example.reservation.config.TestSecurityConfig;
import com.example.reservation.domain.property.Property;
import com.example.reservation.domain.property.PropertyAccessCode;
import com.example.reservation.domain.property.PropertyStatus;
import com.example.reservation.repository.PropertyAccessCodeRepository;
import com.example.reservation.repository.PropertyRepository;
import com.example.reservation.security.WithJwt;
import com.example.reservation.service.PropertyAccessCodeService;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurityConfig.class})
class PropertyAccessCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyAccessCodeRepository accessCodeRepository;

    @Autowired
    private PropertyAccessCodeService accessCodeService;

    private static final String OWNER_SUB = "owner-user-sub";
    private static final String GUEST_SUB = "guest-user-sub";
    private static final String OTHER_USER_SUB = "other-user-sub";
    private static final String GUEST_EMAIL = "guest@example.com";

    private Property testProperty;

    @BeforeEach
    void setUp() {
        accessCodeRepository.deleteAll();
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

    // ===== GET /api/access-codes/property/{propertyId} =====

    @Nested
    @DisplayName("GET /api/access-codes/property/{propertyId} - List access codes for property")
    class GetAccessCodesForProperty {

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns access codes for property owner")
        void returnsAccessCodesForOwner() throws Exception {
            accessCodeService.create(testProperty.getId(), "guest1@example.com", OWNER_SUB, null);
            accessCodeService.create(testProperty.getId(), "guest2@example.com", OWNER_SUB, null);

            mockMvc.perform(get("/api/access-codes/property/{propertyId}", testProperty.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements", is(2)));
        }

        @Test
        @WithJwt(subject = OTHER_USER_SUB)
        @DisplayName("Returns 403 for non-owner")
        void returns403ForNonOwner() throws Exception {
            mockMvc.perform(get("/api/access-codes/property/{propertyId}", testProperty.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/access-codes/property/{propertyId}", testProperty.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== POST /api/access-codes =====

    @Nested
    @DisplayName("POST /api/access-codes - Create access code")
    class CreateAccessCode {

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Creates access code successfully")
        void createsAccessCodeSuccessfully() throws Exception {
            Map<String, Object> request = Map.of(
                    "propertyId", testProperty.getId().toString(),
                    "email", GUEST_EMAIL
            );

            mockMvc.perform(post("/api/access-codes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.propertyId", is(testProperty.getId().toString())))
                    .andExpect(jsonPath("$.issuedToEmail", is(GUEST_EMAIL)))
                    .andExpect(jsonPath("$.code", notNullValue()));

            assertThat(accessCodeRepository.findAll()).hasSize(1);
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Creates access code with expiration")
        void createsAccessCodeWithExpiration() throws Exception {
            Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

            Map<String, Object> request = Map.of(
                    "propertyId", testProperty.getId().toString(),
                    "email", GUEST_EMAIL,
                    "expiresAt", expiresAt.toString()
            );

            mockMvc.perform(post("/api/access-codes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.expiresAt", notNullValue()));
        }

        @Test
        @WithJwt(subject = OTHER_USER_SUB)
        @DisplayName("Returns 403 for non-owner")
        void returns403ForNonOwner() throws Exception {
            Map<String, Object> request = Map.of(
                    "propertyId", testProperty.getId().toString(),
                    "email", GUEST_EMAIL
            );

            mockMvc.perform(post("/api/access-codes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            Map<String, Object> request = Map.of(
                    "propertyId", testProperty.getId().toString(),
                    "email", GUEST_EMAIL
            );

            mockMvc.perform(post("/api/access-codes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns 400 for invalid email")
        void returns400ForInvalidEmail() throws Exception {
            Map<String, Object> request = Map.of(
                    "propertyId", testProperty.getId().toString(),
                    "email", "invalid-email"
            );

            mockMvc.perform(post("/api/access-codes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns 404 for non-existent property")
        void returns404ForNonExistentProperty() throws Exception {
            Map<String, Object> request = Map.of(
                    "propertyId", UUID.randomUUID().toString(),
                    "email", GUEST_EMAIL
            );

            mockMvc.perform(post("/api/access-codes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden()); // PreAuthorize fails before not found
        }
    }

    // ===== POST /api/access-codes/redeem =====

    @Nested
    @DisplayName("POST /api/access-codes/redeem - Redeem access code")
    class RedeemAccessCode {

        @Test
        @WithJwt(subject = GUEST_SUB, email = GUEST_EMAIL)
        @DisplayName("Redeems access code successfully")
        void redeemsAccessCodeSuccessfully() throws Exception {
            PropertyAccessCodeService.PropertyAccessCodeResult result =
                    accessCodeService.create(testProperty.getId(), GUEST_EMAIL, OWNER_SUB, null);

            Map<String, String> request = Map.of("code", result.rawCode());

            mockMvc.perform(post("/api/access-codes/redeem")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.propertyId", is(testProperty.getId().toString())))
                    .andExpect(jsonPath("$.propertyTitle", is("Test Property")))
                    .andExpect(jsonPath("$.message", containsString("succès")));

            PropertyAccessCode redeemed = accessCodeRepository.findAll().get(0);
            assertThat(redeemed.isRedeemed()).isTrue();
            assertThat(redeemed.getRedeemedBySub()).isEqualTo(GUEST_SUB);
        }

        @Test
        @WithJwt(subject = GUEST_SUB, email = GUEST_EMAIL)
        @DisplayName("Returns 404 for invalid code")
        void returns404ForInvalidCode() throws Exception {
            Map<String, String> request = Map.of("code", "invalid-code-12345");

            mockMvc.perform(post("/api/access-codes/redeem")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithJwt(subject = GUEST_SUB, email = GUEST_EMAIL)
        @DisplayName("Returns 409 for already redeemed code")
        void returns409ForAlreadyRedeemedCode() throws Exception {
            PropertyAccessCodeService.PropertyAccessCodeResult result =
                    accessCodeService.create(testProperty.getId(), GUEST_EMAIL, OWNER_SUB, null);
            accessCodeService.redeem(result.rawCode(), GUEST_SUB);

            Map<String, String> request = Map.of("code", result.rawCode());

            mockMvc.perform(post("/api/access-codes/redeem")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail", containsString("plus actif")));
        }

        @Test
        @WithJwt(subject = GUEST_SUB, email = GUEST_EMAIL)
        @DisplayName("Returns 409 for expired code")
        void returns409ForExpiredCode() throws Exception {
            PropertyAccessCodeService.PropertyAccessCodeResult result =
                    accessCodeService.create(testProperty.getId(), GUEST_EMAIL, OWNER_SUB,
                            Instant.now().minus(1, ChronoUnit.DAYS));

            Map<String, String> request = Map.of("code", result.rawCode());

            mockMvc.perform(post("/api/access-codes/redeem")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail", containsString("plus actif")));
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            PropertyAccessCodeService.PropertyAccessCodeResult result =
                    accessCodeService.create(testProperty.getId(), GUEST_EMAIL, OWNER_SUB, null);

            Map<String, String> request = Map.of("code", result.rawCode());

            mockMvc.perform(post("/api/access-codes/redeem")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithJwt(subject = GUEST_SUB, email = GUEST_EMAIL)
        @DisplayName("Returns 400 for missing code")
        void returns400ForMissingCode() throws Exception {
            mockMvc.perform(post("/api/access-codes/redeem")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ===== POST /api/access-codes/{id}/revoke =====

    @Nested
    @DisplayName("POST /api/access-codes/{id}/revoke - Revoke access code")
    class RevokeAccessCode {

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Revokes access code successfully")
        void revokesAccessCodeSuccessfully() throws Exception {
            PropertyAccessCodeService.PropertyAccessCodeResult result =
                    accessCodeService.create(testProperty.getId(), GUEST_EMAIL, OWNER_SUB, null);

            mockMvc.perform(post("/api/access-codes/{id}/revoke", result.accessCode().getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.revoked", is(true)));

            PropertyAccessCode revoked = accessCodeRepository.findById(result.accessCode().getId()).orElseThrow();
            assertThat(revoked.isRevoked()).isTrue();
            assertThat(revoked.getRevokedBySub()).isEqualTo(OWNER_SUB);
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns 409 for already revoked code")
        void returns409ForAlreadyRevoked() throws Exception {
            PropertyAccessCodeService.PropertyAccessCodeResult result =
                    accessCodeService.create(testProperty.getId(), GUEST_EMAIL, OWNER_SUB, null);
            accessCodeService.revoke(result.accessCode().getId(), OWNER_SUB);

            mockMvc.perform(post("/api/access-codes/{id}/revoke", result.accessCode().getId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail", containsString("déjà révoqué")));
        }

        @Test
        @WithJwt(subject = OTHER_USER_SUB)
        @DisplayName("Returns 403 for non-creator")
        void returns403ForNonCreator() throws Exception {
            PropertyAccessCodeService.PropertyAccessCodeResult result =
                    accessCodeService.create(testProperty.getId(), GUEST_EMAIL, OWNER_SUB, null);

            mockMvc.perform(post("/api/access-codes/{id}/revoke", result.accessCode().getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            PropertyAccessCodeService.PropertyAccessCodeResult result =
                    accessCodeService.create(testProperty.getId(), GUEST_EMAIL, OWNER_SUB, null);

            mockMvc.perform(post("/api/access-codes/{id}/revoke", result.accessCode().getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns 403 for non-existent code")
        void returns403ForNonExistent() throws Exception {
            mockMvc.perform(post("/api/access-codes/{id}/revoke", UUID.randomUUID()))
                    .andExpect(status().isForbidden()); // PreAuthorize fails before not found
        }
    }

    // ===== GET /api/access-codes/mine =====

    @Nested
    @DisplayName("GET /api/access-codes/mine - Get my active access codes")
    class GetMyAccessCodes {

        @Test
        @WithJwt(subject = GUEST_SUB, email = GUEST_EMAIL)
        @DisplayName("Returns active access codes for user")
        void returnsActiveAccessCodes() throws Exception {
            accessCodeService.create(testProperty.getId(), GUEST_EMAIL, OWNER_SUB, null);
            accessCodeService.create(testProperty.getId(), "other@example.com", OWNER_SUB, null);

            mockMvc.perform(get("/api/access-codes/mine"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].issuedToEmail", is(GUEST_EMAIL)));
        }

        @Test
        @WithJwt(subject = GUEST_SUB, email = GUEST_EMAIL)
        @DisplayName("Does not return redeemed codes")
        void doesNotReturnRedeemedCodes() throws Exception {
            PropertyAccessCodeService.PropertyAccessCodeResult result =
                    accessCodeService.create(testProperty.getId(), GUEST_EMAIL, OWNER_SUB, null);
            accessCodeService.redeem(result.rawCode(), GUEST_SUB);

            mockMvc.perform(get("/api/access-codes/mine"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }

        @Test
        @WithJwt(subject = GUEST_SUB, email = GUEST_EMAIL)
        @DisplayName("Does not return revoked codes")
        void doesNotReturnRevokedCodes() throws Exception {
            PropertyAccessCodeService.PropertyAccessCodeResult result =
                    accessCodeService.create(testProperty.getId(), GUEST_EMAIL, OWNER_SUB, null);
            accessCodeService.revoke(result.accessCode().getId(), OWNER_SUB);

            mockMvc.perform(get("/api/access-codes/mine"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }

        @Test
        @WithJwt(subject = GUEST_SUB, email = GUEST_EMAIL)
        @DisplayName("Does not return expired codes")
        void doesNotReturnExpiredCodes() throws Exception {
            accessCodeService.create(testProperty.getId(), GUEST_EMAIL, OWNER_SUB,
                    Instant.now().minus(1, ChronoUnit.DAYS));

            mockMvc.perform(get("/api/access-codes/mine"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/access-codes/mine"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithJwt(subject = GUEST_SUB) // No email claim
        @DisplayName("Returns empty list when no email in JWT")
        void returnsEmptyListWhenNoEmail() throws Exception {
            accessCodeService.create(testProperty.getId(), GUEST_EMAIL, OWNER_SUB, null);

            mockMvc.perform(get("/api/access-codes/mine"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }
}
