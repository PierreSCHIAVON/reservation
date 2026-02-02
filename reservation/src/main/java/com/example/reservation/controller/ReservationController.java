package com.example.reservation.controller;

import com.example.reservation.domain.reservation.Reservation;
import com.example.reservation.dto.ReservationDto;
import com.example.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // ===== Tenant endpoints =====

    @GetMapping("/mine")
    public List<ReservationDto.ListResponse> getMyReservations(@AuthenticationPrincipal Jwt jwt) {
        return reservationService.findByTenant(jwt.getSubject()).stream()
                .map(ReservationDto.ListResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ReservationDto.Response> createReservation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReservationDto.CreateRequest request
    ) {
        Reservation reservation = reservationService.create(
                request.propertyId(),
                jwt.getSubject(),
                request.startDate(),
                request.endDate()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReservationDto.Response.from(reservation));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@authz.canAccessReservation(#id, authentication.name)")
    public ReservationDto.Response cancelReservation(@PathVariable UUID id) {
        return ReservationDto.Response.from(reservationService.cancel(id));
    }

    // ===== Owner endpoints =====

    @GetMapping("/owner")
    public List<ReservationDto.ListResponse> getReservationsForMyProperties(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return reservationService.findByPropertyOwner(jwt.getSubject()).stream()
                .map(ReservationDto.ListResponse::from)
                .toList();
    }

    @GetMapping("/owner/pending")
    public List<ReservationDto.ListResponse> getPendingReservationsForMyProperties(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return reservationService.findPendingByPropertyOwner(jwt.getSubject()).stream()
                .map(ReservationDto.ListResponse::from)
                .toList();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@authz.isReservationPropertyOwner(#id, authentication.name)")
    public ReservationDto.Response confirmReservation(@PathVariable UUID id) {
        return ReservationDto.Response.from(reservationService.confirm(id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("@authz.isReservationPropertyOwner(#id, authentication.name)")
    public ReservationDto.Response completeReservation(@PathVariable UUID id) {
        return ReservationDto.Response.from(reservationService.complete(id));
    }

    @PostMapping("/{id}/discount")
    @PreAuthorize("@authz.isReservationPropertyOwner(#id, authentication.name)")
    public ReservationDto.Response applyDiscount(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody ReservationDto.DiscountRequest request
    ) {
        return ReservationDto.Response.from(
                reservationService.applyDiscount(id, request.discountedUnitPrice(), request.reason(), jwt.getSubject())
        );
    }

    @PostMapping("/{id}/free")
    @PreAuthorize("@authz.isReservationPropertyOwner(#id, authentication.name)")
    public ReservationDto.Response applyFreeStay(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody ReservationDto.FreeStayRequest request
    ) {
        return ReservationDto.Response.from(
                reservationService.applyFreeStay(id, request.reason(), jwt.getSubject())
        );
    }

    // ===== Common endpoints =====

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canAccessReservation(#id, authentication.name)")
    public ReservationDto.Response getReservation(@PathVariable UUID id) {
        return ReservationDto.Response.from(reservationService.findById(id));
    }
}
