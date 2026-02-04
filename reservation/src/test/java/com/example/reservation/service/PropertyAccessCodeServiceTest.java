package com.example.reservation.service;

import com.example.reservation.domain.property.Property;
import com.example.reservation.domain.property.PropertyAccessCode;
import com.example.reservation.domain.property.PropertyStatus;
import com.example.reservation.repository.PropertyAccessCodeRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PropertyAccessCodeService Unit Tests")
class PropertyAccessCodeServiceTest {

    @Mock
    private PropertyAccessCodeRepository accessCodeRepository;

    @Mock
    private PropertyService propertyService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PropertyAccessCodeService accessCodeService;

    private static final UUID ACCESS_CODE_ID = UUID.randomUUID();
    private static final UUID PROPERTY_ID = UUID.randomUUID();
    private static final String CREATOR_SUB = "creator-123";
    private static final String USER_SUB = "user-456";
    private static final String EMAIL = "test@example.com";
    private static final String CODE_LOOKUP = "abc123def456";
    private static final String CODE_HASH = "$2a$10$hashedcode";

    private Property createTestProperty() {
        return Property.builder()
                .id(PROPERTY_ID)
                .ownerSub(CREATOR_SUB)
                .title("Test Property")
                .description("Test Description")
                .city("Paris")
                .pricePerNight(new BigDecimal("100.00"))
                .status(PropertyStatus.ACTIVE)
                .build();
    }

    private PropertyAccessCode createTestAccessCode(Property property) {
        return PropertyAccessCode.builder()
                .id(ACCESS_CODE_ID)
                .property(property)
                .issuedToEmail(EMAIL)
                .codeLookup(CODE_LOOKUP)
                .codeHash(CODE_HASH)
                .createdBySub(CREATOR_SUB)
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Should return access code when found")
        void shouldReturnAccessCodeWhenFound() {
            PropertyAccessCode accessCode = createTestAccessCode(createTestProperty());
            when(accessCodeRepository.findById(ACCESS_CODE_ID)).thenReturn(Optional.of(accessCode));

            PropertyAccessCode result = accessCodeService.findById(ACCESS_CODE_ID);

            assertThat(result).isEqualTo(accessCode);
            verify(accessCodeRepository).findById(ACCESS_CODE_ID);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(accessCodeRepository.findById(ACCESS_CODE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accessCodeService.findById(ACCESS_CODE_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Access code not found");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Should create access code with correct encryption")
        void shouldCreateAccessCodeWithCorrectEncryption() {
            Property property = createTestProperty();
            Instant expiresAt = Instant.now().plusSeconds(86400);
            when(propertyService.findById(PROPERTY_ID)).thenReturn(property);
            when(passwordEncoder.encode(anyString())).thenReturn(CODE_HASH);
            when(accessCodeRepository.save(any(PropertyAccessCode.class)))
                    .thenAnswer(invocation -> {
                        PropertyAccessCode code = invocation.getArgument(0);
                        code.setId(ACCESS_CODE_ID);
                        return code;
                    });

            PropertyAccessCodeService.PropertyAccessCodeResult result = accessCodeService.create(
                    PROPERTY_ID, EMAIL, CREATOR_SUB, expiresAt
            );

            ArgumentCaptor<PropertyAccessCode> captor = ArgumentCaptor.forClass(PropertyAccessCode.class);
            verify(accessCodeRepository).save(captor.capture());

            PropertyAccessCode saved = captor.getValue();
            assertThat(saved.getProperty()).isEqualTo(property);
            assertThat(saved.getIssuedToEmail()).isEqualTo(EMAIL.toLowerCase());
            assertThat(saved.getCodeLookup()).isNotNull();
            assertThat(saved.getCodeHash()).isEqualTo(CODE_HASH);
            assertThat(saved.getCreatedBySub()).isEqualTo(CREATOR_SUB);
            assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);

            assertThat(result.rawCode()).isNotNull();
            assertThat(result.rawCode()).isNotEmpty();
            assertThat(result.accessCode()).isEqualTo(saved);
        }

        @Test
        @DisplayName("Should normalize email to lowercase")
        void shouldNormalizeEmailToLowercase() {
            Property property = createTestProperty();
            when(propertyService.findById(PROPERTY_ID)).thenReturn(property);
            when(passwordEncoder.encode(anyString())).thenReturn(CODE_HASH);
            when(accessCodeRepository.save(any(PropertyAccessCode.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            accessCodeService.create(PROPERTY_ID, "Test@EXAMPLE.COM", CREATOR_SUB, null);

            ArgumentCaptor<PropertyAccessCode> captor = ArgumentCaptor.forClass(PropertyAccessCode.class);
            verify(accessCodeRepository).save(captor.capture());

            assertThat(captor.getValue().getIssuedToEmail()).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("redeem")
    class Redeem {

        @Test
        @DisplayName("Should redeem valid access code")
        void shouldRedeemValidAccessCode() {
            String rawCode = "valid-raw-code";
            Property property = createTestProperty();
            PropertyAccessCode accessCode = createTestAccessCode(property);

            when(accessCodeRepository.findByCodeLookup(anyString())).thenReturn(Optional.of(accessCode));
            when(passwordEncoder.matches(rawCode, CODE_HASH)).thenReturn(true);
            when(accessCodeRepository.save(any(PropertyAccessCode.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            PropertyAccessCode result = accessCodeService.redeem(rawCode, USER_SUB, EMAIL);

            assertThat(result.getRedeemedAt()).isNotNull();
            assertThat(result.getRedeemedBySub()).isEqualTo(USER_SUB);
            verify(accessCodeRepository).save(accessCode);
        }

        @Test
        @DisplayName("Should throw when email is null")
        void shouldThrowWhenEmailIsNull() {
            assertThatThrownBy(() -> accessCodeService.redeem("code", USER_SUB, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email manquant");

            verify(accessCodeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when email is blank")
        void shouldThrowWhenEmailIsBlank() {
            assertThatThrownBy(() -> accessCodeService.redeem("code", USER_SUB, "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email manquant");
        }

        @Test
        @DisplayName("Should throw when code not found")
        void shouldThrowWhenCodeNotFound() {
            String rawCode = "invalid-code";
            when(accessCodeRepository.findByCodeLookup(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accessCodeService.redeem(rawCode, USER_SUB, EMAIL))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("invalide");
        }

        @Test
        @DisplayName("Should throw when email does not match")
        void shouldThrowWhenEmailDoesNotMatch() {
            String rawCode = "valid-code";
            Property property = createTestProperty();
            PropertyAccessCode accessCode = createTestAccessCode(property);

            when(accessCodeRepository.findByCodeLookup(anyString())).thenReturn(Optional.of(accessCode));

            assertThatThrownBy(() -> accessCodeService.redeem(rawCode, USER_SUB, "wrong@example.com"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("invalide");
        }

        @Test
        @DisplayName("Should throw when code is not active")
        void shouldThrowWhenCodeNotActive() {
            String rawCode = "valid-code";
            Property property = createTestProperty();
            PropertyAccessCode accessCode = createTestAccessCode(property);
            accessCode.setRevokedAt(Instant.now());

            when(accessCodeRepository.findByCodeLookup(anyString())).thenReturn(Optional.of(accessCode));

            assertThatThrownBy(() -> accessCodeService.redeem(rawCode, USER_SUB, EMAIL))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("n'est plus actif");
        }

        @Test
        @DisplayName("Should throw when password does not match")
        void shouldThrowWhenPasswordDoesNotMatch() {
            String rawCode = "wrong-code";
            Property property = createTestProperty();
            PropertyAccessCode accessCode = createTestAccessCode(property);

            when(accessCodeRepository.findByCodeLookup(anyString())).thenReturn(Optional.of(accessCode));
            when(passwordEncoder.matches(rawCode, CODE_HASH)).thenReturn(false);

            assertThatThrownBy(() -> accessCodeService.redeem(rawCode, USER_SUB, EMAIL))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("invalide");
        }
    }

    @Nested
    @DisplayName("revoke")
    class Revoke {

        @Test
        @DisplayName("Should revoke access code")
        void shouldRevokeAccessCode() {
            Property property = createTestProperty();
            PropertyAccessCode accessCode = createTestAccessCode(property);

            when(accessCodeRepository.findById(ACCESS_CODE_ID)).thenReturn(Optional.of(accessCode));
            when(accessCodeRepository.save(any(PropertyAccessCode.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            PropertyAccessCode result = accessCodeService.revoke(ACCESS_CODE_ID, CREATOR_SUB);

            assertThat(result.getRevokedAt()).isNotNull();
            assertThat(result.getRevokedBySub()).isEqualTo(CREATOR_SUB);
            verify(accessCodeRepository).save(accessCode);
        }

        @Test
        @DisplayName("Should throw when already revoked")
        void shouldThrowWhenAlreadyRevoked() {
            Property property = createTestProperty();
            PropertyAccessCode accessCode = createTestAccessCode(property);
            accessCode.setRevokedAt(Instant.now());

            when(accessCodeRepository.findById(ACCESS_CODE_ID)).thenReturn(Optional.of(accessCode));

            assertThatThrownBy(() -> accessCodeService.revoke(ACCESS_CODE_ID, CREATOR_SUB))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("déjà révoqué");

            verify(accessCodeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("validateCode")
    class ValidateCode {

        @Test
        @DisplayName("Should return true for valid active code")
        void shouldReturnTrueForValidActiveCode() {
            String rawCode = "valid-code";
            Property property = createTestProperty();
            PropertyAccessCode accessCode = createTestAccessCode(property);

            when(accessCodeRepository.findByCodeLookup(anyString())).thenReturn(Optional.of(accessCode));
            when(passwordEncoder.matches(rawCode, CODE_HASH)).thenReturn(true);

            boolean result = accessCodeService.validateCode(rawCode);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-existent code")
        void shouldReturnFalseForNonExistentCode() {
            when(accessCodeRepository.findByCodeLookup(anyString())).thenReturn(Optional.empty());

            boolean result = accessCodeService.validateCode("invalid-code");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for inactive code")
        void shouldReturnFalseForInactiveCode() {
            String rawCode = "code";
            Property property = createTestProperty();
            PropertyAccessCode accessCode = createTestAccessCode(property);
            accessCode.setRevokedAt(Instant.now());

            when(accessCodeRepository.findByCodeLookup(anyString())).thenReturn(Optional.of(accessCode));

            boolean result = accessCodeService.validateCode(rawCode);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for wrong password")
        void shouldReturnFalseForWrongPassword() {
            String rawCode = "wrong-code";
            Property property = createTestProperty();
            PropertyAccessCode accessCode = createTestAccessCode(property);

            when(accessCodeRepository.findByCodeLookup(anyString())).thenReturn(Optional.of(accessCode));
            when(passwordEncoder.matches(rawCode, CODE_HASH)).thenReturn(false);

            boolean result = accessCodeService.validateCode(rawCode);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isCreator")
    class IsCreator {

        @Test
        @DisplayName("Should return true when user is creator")
        void shouldReturnTrueWhenUserIsCreator() {
            Property property = createTestProperty();
            PropertyAccessCode accessCode = createTestAccessCode(property);

            when(accessCodeRepository.findById(ACCESS_CODE_ID)).thenReturn(Optional.of(accessCode));

            boolean result = accessCodeService.isCreator(ACCESS_CODE_ID, CREATOR_SUB);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when user is not creator")
        void shouldReturnFalseWhenUserIsNotCreator() {
            Property property = createTestProperty();
            PropertyAccessCode accessCode = createTestAccessCode(property);

            when(accessCodeRepository.findById(ACCESS_CODE_ID)).thenReturn(Optional.of(accessCode));

            boolean result = accessCodeService.isCreator(ACCESS_CODE_ID, "other-user");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Query Methods")
    class QueryMethods {

        @Test
        @DisplayName("findByProperty should return all codes for property")
        void findByPropertyShouldReturnAllCodes() {
            List<PropertyAccessCode> expected = List.of(
                    createTestAccessCode(createTestProperty())
            );
            when(accessCodeRepository.findByPropertyId(PROPERTY_ID)).thenReturn(expected);

            List<PropertyAccessCode> result = accessCodeService.findByProperty(PROPERTY_ID);

            assertThat(result).hasSize(1);
            verify(accessCodeRepository).findByPropertyId(PROPERTY_ID);
        }

        @Test
        @DisplayName("findActiveByProperty should return only active codes")
        void findActiveByPropertyShouldReturnOnlyActiveCodes() {
            List<PropertyAccessCode> expected = List.of(
                    createTestAccessCode(createTestProperty())
            );
            when(accessCodeRepository.findActiveByPropertyId(PROPERTY_ID)).thenReturn(expected);

            List<PropertyAccessCode> result = accessCodeService.findActiveByProperty(PROPERTY_ID);

            assertThat(result).hasSize(1);
            verify(accessCodeRepository).findActiveByPropertyId(PROPERTY_ID);
        }

        @Test
        @DisplayName("findActiveByEmail should return codes for email")
        void findActiveByEmailShouldReturnCodesForEmail() {
            List<PropertyAccessCode> expected = List.of(
                    createTestAccessCode(createTestProperty())
            );
            when(accessCodeRepository.findActiveByEmail(EMAIL)).thenReturn(expected);

            List<PropertyAccessCode> result = accessCodeService.findActiveByEmail(EMAIL);

            assertThat(result).hasSize(1);
            verify(accessCodeRepository).findActiveByEmail(EMAIL);
        }
    }

    @Nested
    @DisplayName("Paginated Methods")
    class PaginatedMethods {

        @Test
        @DisplayName("findByProperty paginated should maintain order")
        void findByPropertyPaginatedShouldMaintainOrder() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 20);
            Page<UUID> idsPage = new PageImpl<>(List.of(id1, id2), pageable, 2);

            Property property = createTestProperty();
            PropertyAccessCode code1 = createTestAccessCode(property);
            code1.setId(id1);
            PropertyAccessCode code2 = createTestAccessCode(property);
            code2.setId(id2);

            when(accessCodeRepository.findIdsByPropertyId(PROPERTY_ID, pageable))
                    .thenReturn(idsPage);
            when(accessCodeRepository.findByIdsWithProperty(List.of(id1, id2)))
                    .thenReturn(List.of(code2, code1)); // Intentionally reversed

            Page<PropertyAccessCode> result = accessCodeService.findByProperty(PROPERTY_ID, pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getId()).isEqualTo(id1);
            assertThat(result.getContent().get(1).getId()).isEqualTo(id2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }
    }
}
