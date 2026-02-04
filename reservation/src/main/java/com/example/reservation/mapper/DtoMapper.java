package com.example.reservation.mapper;

import com.example.reservation.domain.property.Property;
import com.example.reservation.domain.property.PropertyAccessCode;
import com.example.reservation.domain.reservation.Reservation;
import com.example.reservation.dto.generated.PageResponsePropertyAccessCodeResponse;
import com.example.reservation.dto.generated.PageResponsePropertyListResponse;
import com.example.reservation.dto.generated.PageResponseReservationListResponse;
import com.example.reservation.dto.generated.PropertyAccessCodeCreateResponse;
import com.example.reservation.dto.generated.PropertyAccessCodeRedeemResponse;
import com.example.reservation.dto.generated.PropertyAccessCodeResponse;
import com.example.reservation.dto.generated.PropertyListResponse;
import com.example.reservation.dto.generated.PropertyResponse;
import com.example.reservation.dto.generated.ReservationListResponse;
import com.example.reservation.dto.generated.ReservationResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.Page;

public final class DtoMapper {

    private DtoMapper() {}

    public static PropertyResponse toPropertyResponse(Property property) {
        return new PropertyResponse(
                property.getId(),
                property.getOwnerSub(),
                property.getTitle(),
                property.getDescription(),
                property.getCity(),
                property.getPricePerNight(),
                com.example.reservation.dto.generated.PropertyStatus.fromValue(property.getStatus().name()),
                toOffsetDateTime(property.getCreatedAt()),
                toOffsetDateTime(property.getUpdatedAt())
        );
    }

    public static PropertyListResponse toPropertyListResponse(Property property) {
        return new PropertyListResponse(
                property.getId(),
                property.getTitle(),
                property.getCity(),
                property.getPricePerNight(),
                com.example.reservation.dto.generated.PropertyStatus.fromValue(property.getStatus().name())
        );
    }

    public static ReservationResponse toReservationResponse(Reservation reservation) {
        ReservationResponse response = new ReservationResponse(
                reservation.getId(),
                reservation.getProperty().getId(),
                reservation.getProperty().getTitle(),
                reservation.getTenantSub(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                reservation.getNights(),
                com.example.reservation.dto.generated.ReservationStatus.fromValue(reservation.getStatus().name()),
                reservation.getUnitPriceApplied(),
                reservation.getTotalPrice(),
                com.example.reservation.dto.generated.PricingType.fromValue(reservation.getPricingType().name()),
                toOffsetDateTime(reservation.getCreatedAt()),
                toOffsetDateTime(reservation.getUpdatedAt())
        );
        response.setPricingReason(reservation.getPricingReason());
        return response;
    }

    public static ReservationListResponse toReservationListResponse(Reservation reservation) {
        return new ReservationListResponse(
                reservation.getId(),
                reservation.getProperty().getId(),
                reservation.getProperty().getTitle(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                com.example.reservation.dto.generated.ReservationStatus.fromValue(reservation.getStatus().name()),
                reservation.getTotalPrice()
        );
    }

    public static PropertyAccessCodeResponse toPropertyAccessCodeResponse(PropertyAccessCode code) {
        PropertyAccessCodeResponse response = new PropertyAccessCodeResponse(
                code.getId(),
                code.getProperty().getId(),
                code.getProperty().getTitle(),
                code.getIssuedToEmail(),
                code.getCreatedBySub(),
                toOffsetDateTime(code.getCreatedAt()),
                code.isActive(),
                code.isRedeemed(),
                code.isRevoked(),
                code.isExpired()
        );
        response.setExpiresAt(toOffsetDateTime(code.getExpiresAt()));
        return response;
    }

    public static PropertyAccessCodeCreateResponse toPropertyAccessCodeCreateResponse(PropertyAccessCode code, String rawCode) {
        PropertyAccessCodeCreateResponse response = new PropertyAccessCodeCreateResponse(
                code.getId(),
                code.getProperty().getId(),
                code.getIssuedToEmail(),
                rawCode,
                toOffsetDateTime(code.getCreatedAt())
        );
        response.setExpiresAt(toOffsetDateTime(code.getExpiresAt()));
        return response;
    }

    public static PropertyAccessCodeRedeemResponse toPropertyAccessCodeRedeemResponse(PropertyAccessCode code) {
        return new PropertyAccessCodeRedeemResponse(
                code.getProperty().getId(),
                code.getProperty().getTitle(),
                "Code utilisé avec succès. Vous avez maintenant accès à cette propriété."
        );
    }

    public static PageResponsePropertyListResponse toPropertyListPage(List<Property> properties) {
        List<PropertyListResponse> content = properties.stream()
                .map(DtoMapper::toPropertyListResponse)
                .toList();
        return new PageResponsePropertyListResponse(
                content,
                0,
                content.size(),
                (long) content.size(),
                1,
                true,
                true
        );
    }

    public static PageResponsePropertyListResponse toPropertyListPage(Page<Property> page) {
        List<PropertyListResponse> content = page.getContent().stream()
                .map(DtoMapper::toPropertyListResponse)
                .toList();
        return new PageResponsePropertyListResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public static PageResponseReservationListResponse toReservationListPage(List<Reservation> reservations) {
        List<ReservationListResponse> content = reservations.stream()
                .map(DtoMapper::toReservationListResponse)
                .toList();
        return new PageResponseReservationListResponse(
                content,
                0,
                content.size(),
                (long) content.size(),
                1,
                true,
                true
        );
    }

    public static PageResponseReservationListResponse toReservationListPage(Page<Reservation> page) {
        List<ReservationListResponse> content = page.getContent().stream()
                .map(DtoMapper::toReservationListResponse)
                .toList();
        return new PageResponseReservationListResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public static PageResponsePropertyAccessCodeResponse toAccessCodePage(List<PropertyAccessCode> codes) {
        List<PropertyAccessCodeResponse> content = codes.stream()
                .map(DtoMapper::toPropertyAccessCodeResponse)
                .toList();
        return new PageResponsePropertyAccessCodeResponse(
                content,
                0,
                content.size(),
                (long) content.size(),
                1,
                true,
                true
        );
    }

    public static PageResponsePropertyAccessCodeResponse toAccessCodePage(Page<PropertyAccessCode> page) {
        List<PropertyAccessCodeResponse> content = page.getContent().stream()
                .map(DtoMapper::toPropertyAccessCodeResponse)
                .toList();
        return new PageResponsePropertyAccessCodeResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
