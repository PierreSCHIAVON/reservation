package com.example.reservation.dto;

import com.example.reservation.domain.property.Property;
import com.example.reservation.domain.property.PropertyStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PropertyDto {

    // ===== Request DTOs =====

    public record CreateRequest(
            @NotBlank @Size(max = 120) String title,

            @NotBlank @Size(max = 2000) String description,

            @NotBlank @Size(max = 120) String city,

            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal pricePerNight
    ) {}

    public record UpdateRequest(
            @Size(max = 120) String title,

            @Size(max = 2000) String description,

            @Size(max = 120) String city,

            @DecimalMin(value = "0.0", inclusive = false) BigDecimal pricePerNight
    ) {}

    // ===== Response DTOs =====

    public record Response(
            UUID id,

            String ownerSub,

            String title,

            String description,

            String city,

            BigDecimal pricePerNight,

            PropertyStatus status,

            Instant createdAt,

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

    public record ListResponse(
            UUID id,

            String title,

            String city,

            BigDecimal pricePerNight,

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
