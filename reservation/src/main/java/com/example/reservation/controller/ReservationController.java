package com.example.reservation.controller;

import com.example.reservation.domain.reservation.Reservation;
import com.example.reservation.dto.generated.PageResponseReservationListResponse;
import com.example.reservation.dto.generated.ReservationCreateRequest;
import com.example.reservation.dto.generated.ReservationDiscountRequest;
import com.example.reservation.dto.generated.ReservationFreeStayRequest;
import com.example.reservation.dto.generated.ReservationResponse;
import com.example.reservation.mapper.DtoMapper;
import com.example.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // ===== Tenant endpoints =====

    @GetMapping("/mine")
    public PageResponseReservationListResponse getMyReservations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        if (unpaged) {
            return DtoMapper.toReservationListPage(
                    reservationService.findByTenant(jwt.getSubject())
            );
        }

        return DtoMapper.toReservationListPage(
                reservationService.findByTenant(jwt.getSubject(), pageable)
        );
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReservationCreateRequest request
    ) {
        Reservation reservation = reservationService.create(
                request.getPropertyId(),
                jwt.getSubject(),
                request.getStartDate(),
                request.getEndDate()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DtoMapper.toReservationResponse(reservation));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@authz.canAccessReservation(#id, authentication.principal.subject)")
    public ReservationResponse cancelReservation(@PathVariable UUID id) {
        return DtoMapper.toReservationResponse(reservationService.cancel(id));
    }

    // ===== Owner endpoints =====

    @GetMapping("/owner")
    public PageResponseReservationListResponse getReservationsForMyProperties(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        if (unpaged) {
            return DtoMapper.toReservationListPage(
                    reservationService.findByPropertyOwner(jwt.getSubject())
            );
        }

        return DtoMapper.toReservationListPage(
                reservationService.findByPropertyOwner(jwt.getSubject(), pageable)
        );
    }

    @GetMapping("/owner/pending")
    public PageResponseReservationListResponse getPendingReservationsForMyProperties(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        if (unpaged) {
            return DtoMapper.toReservationListPage(
                    reservationService.findPendingByPropertyOwner(jwt.getSubject())
            );
        }

        return DtoMapper.toReservationListPage(
                reservationService.findPendingByPropertyOwner(jwt.getSubject(), pageable)
        );
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@authz.isReservationPropertyOwner(#id, authentication.principal.subject)")
    public ReservationResponse confirmReservation(@PathVariable UUID id) {
        return DtoMapper.toReservationResponse(reservationService.confirm(id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("@authz.isReservationPropertyOwner(#id, authentication.principal.subject)")
    public ReservationResponse completeReservation(@PathVariable UUID id) {
        return DtoMapper.toReservationResponse(reservationService.complete(id));
    }

    @PostMapping("/{id}/discount")
    @PreAuthorize("@authz.isReservationPropertyOwner(#id, authentication.principal.subject)")
    public ReservationResponse applyDiscount(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody ReservationDiscountRequest request
    ) {
        return DtoMapper.toReservationResponse(
                reservationService.applyDiscount(id, request.getDiscountedUnitPrice(), request.getReason(), jwt.getSubject())
        );
    }

    @PostMapping("/{id}/free")
    @PreAuthorize("@authz.isReservationPropertyOwner(#id, authentication.principal.subject)")
    public ReservationResponse applyFreeStay(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody ReservationFreeStayRequest request
    ) {
        return DtoMapper.toReservationResponse(
                reservationService.applyFreeStay(id, request.getReason(), jwt.getSubject())
        );
    }

    // ===== Common endpoints =====

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canAccessReservation(#id, authentication.principal.subject)")
    public ReservationResponse getReservation(@PathVariable UUID id) {
        return DtoMapper.toReservationResponse(reservationService.findById(id));
    }
}
