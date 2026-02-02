package com.example.reservation.controller;

import com.example.reservation.domain.property.Property;
import com.example.reservation.dto.PropertyDto;
import com.example.reservation.service.PropertyService;
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
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @GetMapping
    public List<PropertyDto.ListResponse> getActiveProperties(
            @RequestParam(required = false) String city
    ) {
        List<Property> properties = city != null
                ? propertyService.findActivePropertiesByCity(city)
                : propertyService.findActiveProperties();

        return properties.stream()
                .map(PropertyDto.ListResponse::from)
                .toList();
    }

    @GetMapping("/mine")
    public List<PropertyDto.ListResponse> getMyProperties(@AuthenticationPrincipal Jwt jwt) {
        return propertyService.findByOwner(jwt.getSubject()).stream()
                .map(PropertyDto.ListResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public PropertyDto.Response getProperty(@PathVariable UUID id) {
        return PropertyDto.Response.from(propertyService.findById(id));
    }

    @PostMapping
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
    public PropertyDto.Response updateProperty(
            @PathVariable UUID id,
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
    public PropertyDto.Response activateProperty(@PathVariable UUID id) {
        return PropertyDto.Response.from(propertyService.activate(id));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("@authz.isPropertyOwner(#id, authentication.name)")
    public PropertyDto.Response deactivateProperty(@PathVariable UUID id) {
        return PropertyDto.Response.from(propertyService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.isPropertyOwner(#id, authentication.name)")
    public ResponseEntity<Void> deleteProperty(@PathVariable UUID id) {
        propertyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
