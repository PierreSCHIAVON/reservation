package com.example.reservation.controller;

import com.example.reservation.domain.property.Property;
import com.example.reservation.dto.PageResponse;
import com.example.reservation.dto.PropertyDto;
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
import org.springframework.data.domain.Page;
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
@RequestMapping("/api/properties")
@RequiredArgsConstructor
@Tag(name = "Properties", description = "Gestion des propriétés")
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping
    @Operation(summary = "Lister les propriétés actives", description = "Retourne la liste paginée des propriétés actives, optionnellement filtrées par ville. Utilisez unpaged=true pour obtenir tous les résultats.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page de propriétés")
    })
    public PageResponse<PropertyDto.ListResponse> getActiveProperties(
            @Parameter(description = "Filtre par ville (insensible à la casse)") @RequestParam(required = false) String city,
            @Parameter(description = "Retourner tous les résultats sans pagination") @RequestParam(defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        if (unpaged) {
            List<Property> properties = city != null
                    ? propertyService.findActivePropertiesByCity(city)
                    : propertyService.findActiveProperties();
            return PageResponse.unpaged(properties.stream().map(PropertyDto.ListResponse::from).toList());
        }

        Page<Property> properties = city != null
                ? propertyService.findActivePropertiesByCity(city, pageable)
                : propertyService.findActiveProperties(pageable);

        return PageResponse.from(properties, PropertyDto.ListResponse::from);
    }

    @GetMapping("/mine")
    @Operation(summary = "Mes propriétés", description = "Retourne les propriétés paginées de l'utilisateur connecté")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page de propriétés de l'utilisateur"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content)
    })
    public PageResponse<PropertyDto.ListResponse> getMyProperties(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Retourner tous les résultats sans pagination") @RequestParam(defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        if (unpaged) {
            return PageResponse.unpaged(
                    propertyService.findByOwner(jwt.getSubject()).stream()
                            .map(PropertyDto.ListResponse::from)
                            .toList()
            );
        }

        return PageResponse.from(
                propertyService.findByOwner(jwt.getSubject(), pageable),
                PropertyDto.ListResponse::from
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détails d'une propriété", description = "Retourne les détails complets d'une propriété")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Détails de la propriété"),
            @ApiResponse(responseCode = "404", description = "Propriété non trouvée", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PropertyDto.Response getProperty(@Parameter(description = "ID de la propriété") @PathVariable UUID id) {
        return PropertyDto.Response.from(propertyService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Créer une propriété", description = "Crée une nouvelle propriété pour l'utilisateur connecté")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Propriété créée"),
            @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content)
    })
    public ResponseEntity<PropertyDto.Response> createProperty(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PropertyDto.CreateRequest request
    ) {
        Property property = propertyService.create(
                jwt.getSubject(),
                request.title(),
                request.description(),
                request.city(),
                request.pricePerNight()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PropertyDto.Response.from(property));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.isPropertyOwner(#id, authentication.name)")
    @Operation(summary = "Mettre à jour une propriété", description = "Met à jour les informations d'une propriété. Seul le propriétaire peut modifier.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Propriété mise à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Non propriétaire", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Propriété non trouvée", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PropertyDto.Response updateProperty(
            @Parameter(description = "ID de la propriété") @PathVariable UUID id,
            @Valid @RequestBody PropertyDto.UpdateRequest request
    ) {
        Property property = propertyService.update(
                id,
                request.title(),
                request.description(),
                request.city(),
                request.pricePerNight()
        );

        return PropertyDto.Response.from(property);
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("@authz.isPropertyOwner(#id, authentication.name)")
    @Operation(summary = "Activer une propriété", description = "Active une propriété pour la rendre disponible à la réservation")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Propriété activée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Non propriétaire", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Déjà active", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PropertyDto.Response activateProperty(@Parameter(description = "ID de la propriété") @PathVariable UUID id) {
        return PropertyDto.Response.from(propertyService.activate(id));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("@authz.isPropertyOwner(#id, authentication.name)")
    @Operation(summary = "Désactiver une propriété", description = "Désactive une propriété pour la retirer de la liste des disponibilités")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Propriété désactivée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Non propriétaire", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Déjà inactive", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PropertyDto.Response deactivateProperty(@Parameter(description = "ID de la propriété") @PathVariable UUID id) {
        return PropertyDto.Response.from(propertyService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.isPropertyOwner(#id, authentication.name)")
    @Operation(summary = "Supprimer une propriété", description = "Supprime définitivement une propriété")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Propriété supprimée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Non propriétaire", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Propriété non trouvée", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> deleteProperty(@Parameter(description = "ID de la propriété") @PathVariable UUID id) {
        propertyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
