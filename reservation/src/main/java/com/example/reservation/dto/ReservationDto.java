package com.example.reservation.dto;

import com.example.reservation.domain.reservation.PricingType;
import com.example.reservation.domain.reservation.Reservation;
import com.example.reservation.domain.reservation.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ReservationDto {

    // ===== Request DTOs =====

    @Schema(description = "Requête de création d'une réservation")
    public record CreateRequest(
            @Schema(description = "ID de la propriété à réserver", example = "550e8400-e29b-41d4-a716-446655440000")
            @NotNull UUID propertyId,

            @Schema(description = "Date d'arrivée", example = "2024-06-15")
            @NotNull @FutureOrPresent(message = "La date de début doit être aujourd'hui ou dans le futur") LocalDate startDate,

            @Schema(description = "Date de départ", example = "2024-06-20")
            @NotNull @Future(message = "La date de fin doit être dans le futur") LocalDate endDate
    ) {
        @AssertTrue(message = "La date de fin doit être après la date de début")
        private boolean isEndDateAfterStartDate() {
            return startDate == null || endDate == null || endDate.isAfter(startDate);
        }
    }

    @Schema(description = "Requête d'application d'une réduction")
    public record DiscountRequest(
            @Schema(description = "Prix unitaire réduit par nuit", example = "80.00", minimum = "0")
            @NotNull @DecimalMin(value = "0.0") BigDecimal discountedUnitPrice,

            @Schema(description = "Raison de la réduction", example = "Client fidèle", maxLength = 255)
            @Size(max = 255) String reason
    ) {}

    @Schema(description = "Requête pour offrir un séjour gratuit")
    public record FreeStayRequest(
            @Schema(description = "Raison du séjour gratuit", example = "Compensation suite à un incident", maxLength = 255)
            @NotBlank @Size(max = 255) String reason
    ) {}

    // ===== Response DTOs =====

    @Schema(description = "Détails complets d'une réservation")
    public record Response(
            @Schema(description = "Identifiant unique de la réservation")
            UUID id,

            @Schema(description = "ID de la propriété réservée")
            UUID propertyId,

            @Schema(description = "Titre de la propriété")
            String propertyTitle,

            @Schema(description = "Identifiant du locataire (Keycloak subject)")
            String tenantSub,

            @Schema(description = "Date d'arrivée")
            LocalDate startDate,

            @Schema(description = "Date de départ")
            LocalDate endDate,

            @Schema(description = "Nombre de nuits", example = "5")
            long nights,

            @Schema(description = "Statut de la réservation")
            ReservationStatus status,

            @Schema(description = "Prix unitaire appliqué par nuit")
            BigDecimal unitPriceApplied,

            @Schema(description = "Prix total du séjour")
            BigDecimal totalPrice,

            @Schema(description = "Type de tarification appliquée")
            PricingType pricingType,

            @Schema(description = "Raison de la tarification spéciale (si applicable)")
            String pricingReason,

            @Schema(description = "Date de création")
            Instant createdAt,

            @Schema(description = "Date de dernière modification")
            Instant updatedAt
    ) {
        public static Response from(Reservation reservation) {
            return new Response(
                    reservation.getId(),
                    reservation.getProperty().getId(),
                    reservation.getProperty().getTitle(),
                    reservation.getTenantSub(),
                    reservation.getStartDate(),
                    reservation.getEndDate(),
                    reservation.getNights(),
                    reservation.getStatus(),
                    reservation.getUnitPriceApplied(),
                    reservation.getTotalPrice(),
                    reservation.getPricingType(),
                    reservation.getPricingReason(),
                    reservation.getCreatedAt(),
                    reservation.getUpdatedAt()
            );
        }
    }

    @Schema(description = "Résumé d'une réservation pour les listes")
    public record ListResponse(
            @Schema(description = "Identifiant unique")
            UUID id,

            @Schema(description = "ID de la propriété")
            UUID propertyId,

            @Schema(description = "Titre de la propriété")
            String propertyTitle,

            @Schema(description = "Date d'arrivée")
            LocalDate startDate,

            @Schema(description = "Date de départ")
            LocalDate endDate,

            @Schema(description = "Statut de la réservation")
            ReservationStatus status,

            @Schema(description = "Prix total")
            BigDecimal totalPrice
    ) {
        public static ListResponse from(Reservation reservation) {
            return new ListResponse(
                    reservation.getId(),
                    reservation.getProperty().getId(),
                    reservation.getProperty().getTitle(),
                    reservation.getStartDate(),
                    reservation.getEndDate(),
                    reservation.getStatus(),
                    reservation.getTotalPrice()
            );
        }
    }
}
