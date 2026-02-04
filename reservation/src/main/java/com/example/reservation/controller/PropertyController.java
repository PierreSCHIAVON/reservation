package com.example.reservation.controller;

import com.example.reservation.domain.property.Property;
import com.example.reservation.dto.generated.PageResponsePropertyListResponse;
import com.example.reservation.dto.generated.PropertyCreateRequest;
import com.example.reservation.dto.generated.PropertyResponse;
import com.example.reservation.dto.generated.PropertyUpdateRequest;
import com.example.reservation.mapper.DtoMapper;
import com.example.reservation.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.reservation.security.annotation.RequiresPropertyOwner;
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
    public PageResponsePropertyListResponse getActiveProperties(
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        if (unpaged) {
            List<Property> properties = city != null
                    ? propertyService.findActivePropertiesByCity(city)
                    : propertyService.findActiveProperties();
            return DtoMapper.toPropertyListPage(properties);
        }

        Page<Property> properties = city != null
                ? propertyService.findActivePropertiesByCity(city, pageable)
                : propertyService.findActiveProperties(pageable);

        return DtoMapper.toPropertyListPage(properties);
    }

    @GetMapping("/mine")
    public PageResponsePropertyListResponse getMyProperties(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        if (unpaged) {
            return DtoMapper.toPropertyListPage(
                    propertyService.findByOwner(jwt.getSubject())
            );
        }

        return DtoMapper.toPropertyListPage(
                propertyService.findByOwner(jwt.getSubject(), pageable)
        );
    }

    @GetMapping("/{id}")
    public PropertyResponse getProperty(@PathVariable UUID id) {
        return DtoMapper.toPropertyResponse(propertyService.findById(id));
    }

    @PostMapping
    public ResponseEntity<PropertyResponse> createProperty(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PropertyCreateRequest request
    ) {
        Property property = propertyService.create(
                jwt.getSubject(),
                request.getTitle(),
                request.getDescription(),
                request.getCity(),
                request.getPricePerNight()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DtoMapper.toPropertyResponse(property));
    }

    @PutMapping("/{id}")
    @RequiresPropertyOwner
    public PropertyResponse updateProperty(
            @PathVariable UUID id,
            @Valid @RequestBody PropertyUpdateRequest request
    ) {
        Property property = propertyService.update(
                id,
                request.getTitle(),
                request.getDescription(),
                request.getCity(),
                request.getPricePerNight()
        );

        return DtoMapper.toPropertyResponse(property);
    }

    @PostMapping("/{id}/activate")
    @RequiresPropertyOwner
    public PropertyResponse activateProperty(@PathVariable UUID id) {
        return DtoMapper.toPropertyResponse(propertyService.activate(id));
    }

    @PostMapping("/{id}/deactivate")
    @RequiresPropertyOwner
    public PropertyResponse deactivateProperty(@PathVariable UUID id) {
        return DtoMapper.toPropertyResponse(propertyService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @RequiresPropertyOwner
    public ResponseEntity<Void> deleteProperty(@PathVariable UUID id) {
        propertyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
