package com.example.reservation.controller;

import com.example.reservation.domain.reservation.Reservation;
import com.example.reservation.dto.PageResponse;
import com.example.reservation.dto.ReservationDto;
import com.example.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
@Tag(name = "Reservations", description = "Gestion des réservations")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    // ===== Tenant endpoints =====

    @GetMapping("/mine")
    @Operation(summary = "Mes réservations", description = "Retourne les réservations paginées de l'utilisateur connecté en tant que locataire")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page de réservations"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content)
    })
    public PageResponse<ReservationDto.ListResponse> getMyReservations(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Retourner tous les résultats sans pagination") @RequestParam(defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        if (unpaged) {
            return PageResponse.unpaged(
                    reservationService.findByTenant(jwt.getSubject()).stream()
                            .map(ReservationDto.ListResponse::from)
                            .toList()
            );
        }

        return PageResponse.from(
                reservationService.findByTenant(jwt.getSubject(), pageable),
                ReservationDto.ListResponse::from
        );
    }

    @PostMapping
    @Operation(summary = "Créer une réservation", description = "Crée une nouvelle réservation pour la propriété spécifiée")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Réservation créée"),
            @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "404", description = "Propriété non trouvée", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Dates non disponibles ou propriété inactive", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
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
    @Operation(summary = "Annuler une réservation", description = "Annule une réservation. Accessible au locataire ou au propriétaire.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Réservation annulée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Non autorisé", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Annulation impossible (déjà annulée ou terminée)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ReservationDto.Response cancelReservation(@Parameter(description = "ID de la réservation") @PathVariable UUID id) {
        return ReservationDto.Response.from(reservationService.cancel(id));
    }

    // ===== Owner endpoints =====

    @GetMapping("/owner")
    @Operation(summary = "Réservations de mes propriétés", description = "Retourne les réservations paginées pour les propriétés de l'utilisateur connecté")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page de réservations"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content)
    })
    public PageResponse<ReservationDto.ListResponse> getReservationsForMyProperties(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Retourner tous les résultats sans pagination") @RequestParam(defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        if (unpaged) {
            return PageResponse.unpaged(
                    reservationService.findByPropertyOwner(jwt.getSubject()).stream()
                            .map(ReservationDto.ListResponse::from)
                            .toList()
            );
        }

        return PageResponse.from(
                reservationService.findByPropertyOwner(jwt.getSubject(), pageable),
                ReservationDto.ListResponse::from
        );
    }

    @GetMapping("/owner/pending")
    @Operation(summary = "Réservations en attente", description = "Retourne les réservations paginées en attente de confirmation pour les propriétés de l'utilisateur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page de réservations en attente"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content)
    })
    public PageResponse<ReservationDto.ListResponse> getPendingReservationsForMyProperties(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Retourner tous les résultats sans pagination") @RequestParam(defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        if (unpaged) {
            return PageResponse.unpaged(
                    reservationService.findPendingByPropertyOwner(jwt.getSubject()).stream()
                            .map(ReservationDto.ListResponse::from)
                            .toList()
            );
        }

        return PageResponse.from(
                reservationService.findPendingByPropertyOwner(jwt.getSubject(), pageable),
                ReservationDto.ListResponse::from
        );
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@authz.isReservationPropertyOwner(#id, authentication.name)")
    @Operation(summary = "Confirmer une réservation", description = "Confirme une réservation en attente. Réservé au propriétaire.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Réservation confirmée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Non propriétaire", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Statut invalide (pas en attente)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ReservationDto.Response confirmReservation(@Parameter(description = "ID de la réservation") @PathVariable UUID id) {
        return ReservationDto.Response.from(reservationService.confirm(id));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("@authz.isReservationPropertyOwner(#id, authentication.name)")
    @Operation(summary = "Terminer une réservation", description = "Marque une réservation confirmée comme terminée. Réservé au propriétaire.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Réservation terminée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Non propriétaire", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Statut invalide (pas confirmée)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ReservationDto.Response completeReservation(@Parameter(description = "ID de la réservation") @PathVariable UUID id) {
        return ReservationDto.Response.from(reservationService.complete(id));
    }

    @PostMapping("/{id}/discount")
    @PreAuthorize("@authz.isReservationPropertyOwner(#id, authentication.name)")
    @Operation(summary = "Appliquer une réduction", description = "Applique un prix réduit à une réservation en attente. Réservé au propriétaire.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Réduction appliquée"),
            @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Non propriétaire", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Statut invalide (pas en attente)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ReservationDto.Response applyDiscount(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID de la réservation") @PathVariable UUID id,
            @Valid @RequestBody ReservationDto.DiscountRequest request
    ) {
        return ReservationDto.Response.from(
                reservationService.applyDiscount(id, request.discountedUnitPrice(), request.reason(), jwt.getSubject())
        );
    }

    @PostMapping("/{id}/free")
    @PreAuthorize("@authz.isReservationPropertyOwner(#id, authentication.name)")
    @Operation(summary = "Offrir un séjour gratuit", description = "Marque une réservation comme gratuite. Réservé au propriétaire.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Séjour gratuit appliqué"),
            @ApiResponse(responseCode = "400", description = "Raison manquante", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Non propriétaire", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Statut invalide (pas en attente)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ReservationDto.Response applyFreeStay(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID de la réservation") @PathVariable UUID id,
            @Valid @RequestBody ReservationDto.FreeStayRequest request
    ) {
        return ReservationDto.Response.from(
                reservationService.applyFreeStay(id, request.reason(), jwt.getSubject())
        );
    }

    // ===== Common endpoints =====

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canAccessReservation(#id, authentication.name)")
    @Operation(summary = "Détails d'une réservation", description = "Retourne les détails d'une réservation. Accessible au locataire ou au propriétaire.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Détails de la réservation"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Non autorisé", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Réservation non trouvée", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ReservationDto.Response getReservation(@Parameter(description = "ID de la réservation") @PathVariable UUID id) {
        return ReservationDto.Response.from(reservationService.findById(id));
    }
}
