package com.example.reservation.dto;

import com.example.reservation.domain.property.Property;
import com.example.reservation.domain.property.PropertyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PropertyDto {

    // ===== Request DTOs =====

    @Schema(description = "Requête de création d'une propriété")
    public record CreateRequest(
            @Schema(description = "Titre de la propriété", example = "Appartement Paris 8ème", maxLength = 120)
            @NotBlank @Size(max = 120) String title,

            @Schema(description = "Description détaillée", example = "Bel appartement lumineux avec vue sur la Tour Eiffel", maxLength = 2000)
            @NotBlank @Size(max = 2000) String description,

            @Schema(description = "Ville", example = "Paris", maxLength = 120)
            @NotBlank @Size(max = 120) String city,

            @Schema(description = "Prix par nuit en euros", example = "150.00", minimum = "0.01")
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal pricePerNight
    ) {}

    @Schema(description = "Requête de mise à jour d'une propriété (tous les champs sont optionnels)")
    public record UpdateRequest(
            @Schema(description = "Nouveau titre", example = "Appartement Paris 8ème rénové", maxLength = 120)
            @Size(max = 120) String title,

            @Schema(description = "Nouvelle description", maxLength = 2000)
            @Size(max = 2000) String description,

            @Schema(description = "Nouvelle ville", example = "Lyon", maxLength = 120)
            @Size(max = 120) String city,

            @Schema(description = "Nouveau prix par nuit", example = "175.00", minimum = "0.01")
            @DecimalMin(value = "0.0", inclusive = false) BigDecimal pricePerNight
    ) {}

    // ===== Response DTOs =====

    @Schema(description = "Détails complets d'une propriété")
    public record Response(
            @Schema(description = "Identifiant unique", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID id,

            @Schema(description = "Identifiant du propriétaire (Keycloak subject)", example = "user-123-abc")
            String ownerSub,

            @Schema(description = "Titre", example = "Appartement Paris 8ème")
            String title,

            @Schema(description = "Description détaillée")
            String description,

            @Schema(description = "Ville", example = "Paris")
            String city,

            @Schema(description = "Prix par nuit", example = "150.00")
            BigDecimal pricePerNight,

            @Schema(description = "Statut de la propriété")
            PropertyStatus status,

            @Schema(description = "Date de création")
            Instant createdAt,

            @Schema(description = "Date de dernière modification")
            Instant updatedAt
    ) {
        public static Response from(Property property) {
            return new Response(
                    property.getId(),
                    property.getOwnerSub(),
                    property.getTitle(),
                    property.getDescription(),
                    property.getCity(),
                    property.getPricePerNight(),
                    property.getStatus(),
                    property.getCreatedAt(),
                    property.getUpdatedAt()
            );
        }
    }

    @Schema(description = "Résumé d'une propriété pour les listes")
    public record ListResponse(
            @Schema(description = "Identifiant unique", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID id,

            @Schema(description = "Titre", example = "Appartement Paris 8ème")
            String title,

            @Schema(description = "Ville", example = "Paris")
            String city,

            @Schema(description = "Prix par nuit", example = "150.00")
            BigDecimal pricePerNight,

            @Schema(description = "Statut de la propriété")
            PropertyStatus status
    ) {
        public static ListResponse from(Property property) {
            return new ListResponse(
                    property.getId(),
                    property.getTitle(),
                    property.getCity(),
                    property.getPricePerNight(),
                    property.getStatus()
            );
        }
    }
}
