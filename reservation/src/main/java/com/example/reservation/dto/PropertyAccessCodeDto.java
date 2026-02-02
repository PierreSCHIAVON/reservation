package com.example.reservation.dto;

import com.example.reservation.domain.property.PropertyAccessCode;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public class PropertyAccessCodeDto {

    // ===== Request DTOs =====

    public record CreateRequest(
            @NotNull UUID propertyId,
            @NotBlank @Email @Size(max = 255) String email,
            Instant expiresAt  // null = jamais
    ) {}

    public record RedeemRequest(
            @NotBlank String code
    ) {}

    // ===== Response DTOs =====

    public record CreateResponse(
            UUID id,
            UUID propertyId,
            String issuedToEmail,
            String code,  // code brut, à envoyer à l'invité
            Instant expiresAt,
            Instant createdAt
    ) {}

    public record Response(
            UUID id,
            UUID propertyId,
            String propertyTitle,
            String issuedToEmail,
            String createdBySub,
            Instant createdAt,
            Instant expiresAt,
            boolean active,
            boolean redeemed,
            boolean revoked,
            boolean expired
    ) {
        public static Response from(PropertyAccessCode code) {
            return new Response(
                    code.getId(),
                    code.getProperty().getId(),
                    code.getProperty().getTitle(),
                    code.getIssuedToEmail(),
                    code.getCreatedBySub(),
                    code.getCreatedAt(),
                    code.getExpiresAt(),
                    code.isActive(),
                    code.isRedeemed(),
                    code.isRevoked(),
                    code.isExpired()
            );
        }
    }

    public record RedeemResponse(
            UUID propertyId,
            String propertyTitle,
            String message
    ) {}
}
