package com.example.reservation.controller;

import com.example.reservation.domain.reservation.Reservation;
import com.example.reservation.dto.ReservationDto;
import com.example.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ReservationDto.Response cancelReservation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        // Le locataire ou le propriétaire peut annuler
        if (!reservationService.isTenant(id, jwt.getSubject()) &&
            !reservationService.isPropertyOwner(id, jwt.getSubject())) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à annuler cette réservation");
        }

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
    public ReservationDto.Response confirmReservation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        if (!reservationService.isPropertyOwner(id, jwt.getSubject())) {
            throw new ForbiddenException("Vous n'êtes pas propriétaire de ce bien");
        }

        return ReservationDto.Response.from(reservationService.confirm(id));
    }

    @PostMapping("/{id}/complete")
    public ReservationDto.Response completeReservation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        if (!reservationService.isPropertyOwner(id, jwt.getSubject())) {
            throw new ForbiddenException("Vous n'êtes pas propriétaire de ce bien");
        }

        return ReservationDto.Response.from(reservationService.complete(id));
    }

    @PostMapping("/{id}/discount")
    public ReservationDto.Response applyDiscount(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody ReservationDto.DiscountRequest request
    ) {
        if (!reservationService.isPropertyOwner(id, jwt.getSubject())) {
            throw new ForbiddenException("Vous n'êtes pas propriétaire de ce bien");
        }

        return ReservationDto.Response.from(
                reservationService.applyDiscount(id, request.discountedUnitPrice(), request.reason(), jwt.getSubject())
        );
    }

    @PostMapping("/{id}/free")
    public ReservationDto.Response applyFreeStay(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody ReservationDto.FreeStayRequest request
    ) {
        if (!reservationService.isPropertyOwner(id, jwt.getSubject())) {
            throw new ForbiddenException("Vous n'êtes pas propriétaire de ce bien");
        }

        return ReservationDto.Response.from(
                reservationService.applyFreeStay(id, request.reason(), jwt.getSubject())
        );
    }

    // ===== Common endpoints =====

    @GetMapping("/{id}")
    public ReservationDto.Response getReservation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        // Le locataire ou le propriétaire peut voir la réservation
        if (!reservationService.isTenant(id, jwt.getSubject()) &&
            !reservationService.isPropertyOwner(id, jwt.getSubject())) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à voir cette réservation");
        }

        return ReservationDto.Response.from(reservationService.findById(id));
    }
}
