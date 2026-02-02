package com.example.reservation.dto;

import com.example.reservation.domain.reservation.PricingType;
import com.example.reservation.domain.reservation.Reservation;
import com.example.reservation.domain.reservation.ReservationStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class ReservationDto {

    // ===== Request DTOs =====

    public record CreateRequest(
            @NotNull UUID propertyId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate
    ) {}

    public record DiscountRequest(
            @NotNull @DecimalMin(value = "0.0") BigDecimal discountedUnitPrice,
            @Size(max = 255) String reason
    ) {}

    public record FreeStayRequest(
            @NotBlank @Size(max = 255) String reason
    ) {}

    // ===== Response DTOs =====

    public record Response(
            UUID id,
            UUID propertyId,
            String propertyTitle,
            String tenantSub,
            LocalDate startDate,
            LocalDate endDate,
            long nights,
            ReservationStatus status,
            BigDecimal unitPriceApplied,
            BigDecimal totalPrice,
            PricingType pricingType,
            String pricingReason,
            Instant createdAt,
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

    public record ListResponse(
            UUID id,
            UUID propertyId,
            String propertyTitle,
            LocalDate startDate,
            LocalDate endDate,
            ReservationStatus status,
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
