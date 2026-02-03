package com.example.reservation.controller;

import com.example.reservation.domain.property.PropertyAccessCode;
import com.example.reservation.dto.PageResponse;
import com.example.reservation.dto.PropertyAccessCodeDto;
import com.example.reservation.service.PropertyAccessCodeService;
import com.example.reservation.service.PropertyService;
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
@RequestMapping("/api/access-codes")
@RequiredArgsConstructor
@Tag(name = "Access Codes", description = "Gestion des codes d'accès aux propriétés")
@SecurityRequirement(name = "bearerAuth")
public class PropertyAccessCodeController {

    private final PropertyAccessCodeService accessCodeService;
    private final PropertyService propertyService;

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("@authz.isPropertyOwner(#propertyId, authentication.name)")
    @Operation(summary = "Codes d'accès d'une propriété", description = "Retourne les codes d'accès paginés d'une propriété. Réservé au propriétaire.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page de codes d'accès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Non propriétaire", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<PropertyAccessCodeDto.Response> getAccessCodesForProperty(
            @Parameter(description = "ID de la propriété") @PathVariable UUID propertyId,
            @Parameter(description = "Retourner tous les résultats sans pagination") @RequestParam(defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        if (unpaged) {
            return PageResponse.unpaged(
                    accessCodeService.findByProperty(propertyId).stream()
                            .map(PropertyAccessCodeDto.Response::from)
                            .toList()
            );
        }

        return PageResponse.from(
                accessCodeService.findByProperty(propertyId, pageable),
                PropertyAccessCodeDto.Response::from
        );
    }

    @PostMapping
    @PreAuthorize("@authz.isPropertyOwner(#request.propertyId(), authentication.name)")
    @Operation(summary = "Créer un code d'accès", description = "Crée un nouveau code d'accès pour inviter quelqu'un à une propriété. Réservé au propriétaire.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Code créé (inclut le code brut à partager)"),
            @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Non propriétaire", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PropertyAccessCodeDto.CreateResponse> createAccessCode(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PropertyAccessCodeDto.CreateRequest request
    ) {
        PropertyAccessCodeService.PropertyAccessCodeResult result = accessCodeService.create(
                request.propertyId(),
                request.email(),
                jwt.getSubject(),
                request.expiresAt()
        );

        PropertyAccessCode code = result.accessCode();
        PropertyAccessCodeDto.CreateResponse response = new PropertyAccessCodeDto.CreateResponse(
                code.getId(),
                code.getProperty().getId(),
                code.getIssuedToEmail(),
                result.rawCode(),  // code brut à envoyer à l'invité
                code.getExpiresAt(),
                code.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/redeem")
    @Operation(summary = "Utiliser un code d'accès", description = "Utilise un code d'accès pour obtenir l'accès à une propriété")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Code utilisé avec succès"),
            @ApiResponse(responseCode = "400", description = "Code manquant", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "404", description = "Code invalide", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Code expiré, révoqué ou déjà utilisé", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PropertyAccessCodeDto.RedeemResponse redeemAccessCode(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PropertyAccessCodeDto.RedeemRequest request
    ) {
        PropertyAccessCode code = accessCodeService.redeem(request.code(), jwt.getSubject());

        return new PropertyAccessCodeDto.RedeemResponse(
                code.getProperty().getId(),
                code.getProperty().getTitle(),
                "Code utilisé avec succès. Vous avez maintenant accès à cette propriété."
        );
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("@authz.isAccessCodeCreator(#id, authentication.name)")
    @Operation(summary = "Révoquer un code d'accès", description = "Révoque un code d'accès pour empêcher son utilisation. Réservé au créateur du code.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Code révoqué"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Non créateur du code", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Déjà révoqué", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PropertyAccessCodeDto.Response revokeAccessCode(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "ID du code d'accès") @PathVariable UUID id
    ) {
        return PropertyAccessCodeDto.Response.from(accessCodeService.revoke(id, jwt.getSubject()));
    }

    @GetMapping("/mine")
    @Operation(summary = "Mes codes d'accès actifs", description = "Retourne les codes d'accès paginés actifs destinés à l'email de l'utilisateur connecté")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page de codes d'accès actifs"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content)
    })
    public PageResponse<PropertyAccessCodeDto.Response> getMyActiveAccessCodes(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Retourner tous les résultats sans pagination") @RequestParam(defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            return PageResponse.unpaged(List.of());
        }

        if (unpaged) {
            return PageResponse.unpaged(
                    accessCodeService.findActiveByEmail(email).stream()
                            .map(PropertyAccessCodeDto.Response::from)
                            .toList()
            );
        }

        return PageResponse.from(
                accessCodeService.findActiveByEmail(email, pageable),
                PropertyAccessCodeDto.Response::from
        );
    }
}
