package com.example.reservation.dto;

import com.example.reservation.domain.property.PropertyAccessCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public class PropertyAccessCodeDto {

    // ===== Request DTOs =====

    @Schema(description = "Requête de création d'un code d'accès")
    public record CreateRequest(
            @Schema(description = "ID de la propriété", example = "550e8400-e29b-41d4-a716-446655440000")
            @NotNull UUID propertyId,

            @Schema(description = "Email de l'invité", example = "guest@example.com", maxLength = 255)
            @NotBlank @Email @Size(max = 255) String email,

            @Schema(description = "Date d'expiration (optionnelle, null = jamais)")
            Instant expiresAt
    ) {}

    @Schema(description = "Requête d'utilisation d'un code d'accès")
    public record RedeemRequest(
            @Schema(description = "Code d'accès reçu par email")
            @NotBlank String code
    ) {}

    // ===== Response DTOs =====

    @Schema(description = "Réponse à la création d'un code d'accès (inclut le code brut)")
    public record CreateResponse(
            @Schema(description = "ID du code d'accès créé")
            UUID id,

            @Schema(description = "ID de la propriété")
            UUID propertyId,

            @Schema(description = "Email de l'invité")
            String issuedToEmail,

            @Schema(description = "Code brut à partager avec l'invité (affiché une seule fois)")
            String code,

            @Schema(description = "Date d'expiration")
            Instant expiresAt,

            @Schema(description = "Date de création")
            Instant createdAt
    ) {}

    @Schema(description = "Informations sur un code d'accès")
    public record Response(
            @Schema(description = "ID du code")
            UUID id,

            @Schema(description = "ID de la propriété")
            UUID propertyId,

            @Schema(description = "Titre de la propriété")
            String propertyTitle,

            @Schema(description = "Email de l'invité")
            String issuedToEmail,

            @Schema(description = "Identifiant du créateur")
            String createdBySub,

            @Schema(description = "Date de création")
            Instant createdAt,

            @Schema(description = "Date d'expiration")
            Instant expiresAt,

            @Schema(description = "Le code est-il encore utilisable ?")
            boolean active,

            @Schema(description = "Le code a-t-il été utilisé ?")
            boolean redeemed,

            @Schema(description = "Le code a-t-il été révoqué ?")
            boolean revoked,

            @Schema(description = "Le code a-t-il expiré ?")
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

    @Schema(description = "Réponse à l'utilisation d'un code d'accès")
    public record RedeemResponse(
            @Schema(description = "ID de la propriété débloquée")
            UUID propertyId,

            @Schema(description = "Titre de la propriété")
            String propertyTitle,

            @Schema(description = "Message de confirmation")
            String message
    ) {}
}
