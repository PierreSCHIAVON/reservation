package com.example.reservation.controller;

import com.example.reservation.domain.property.PropertyAccessCode;
import com.example.reservation.dto.generated.PageResponsePropertyAccessCodeResponse;
import com.example.reservation.dto.generated.PropertyAccessCodeCreateRequest;
import com.example.reservation.dto.generated.PropertyAccessCodeCreateResponse;
import com.example.reservation.dto.generated.PropertyAccessCodeRedeemRequest;
import com.example.reservation.dto.generated.PropertyAccessCodeRedeemResponse;
import com.example.reservation.dto.generated.PropertyAccessCodeResponse;
import com.example.reservation.mapper.DtoMapper;
import com.example.reservation.service.PropertyAccessCodeService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/access-codes")
@RequiredArgsConstructor
public class PropertyAccessCodeController {

    private final PropertyAccessCodeService accessCodeService;

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("@authz.isPropertyOwner(#propertyId, authentication.principal.subject)")
    public PageResponsePropertyAccessCodeResponse getAccessCodesForProperty(
            @PathVariable UUID propertyId,
            @RequestParam(defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        if (unpaged) {
            return DtoMapper.toAccessCodePage(
                    accessCodeService.findByProperty(propertyId)
            );
        }

        return DtoMapper.toAccessCodePage(
                accessCodeService.findByProperty(propertyId, pageable)
        );
    }

    @PostMapping
    @PreAuthorize("@authz.isPropertyOwner(#request.propertyId(), authentication.principal.subject)")
    public ResponseEntity<PropertyAccessCodeCreateResponse> createAccessCode(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PropertyAccessCodeCreateRequest request
    ) {
        PropertyAccessCodeService.PropertyAccessCodeResult result = accessCodeService.create(
                request.getPropertyId(),
                request.getEmail(),
                jwt.getSubject(),
                request.getExpiresAt() != null ? request.getExpiresAt().toInstant() : null
        );

        PropertyAccessCode code = result.accessCode();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DtoMapper.toPropertyAccessCodeCreateResponse(code, result.rawCode()));
    }

    @PostMapping("/redeem")
    public PropertyAccessCodeRedeemResponse redeemAccessCode(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PropertyAccessCodeRedeemRequest request
    ) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Le claim 'email' est manquant dans le JWT. Veuillez vérifier la configuration de Keycloak.");
        }
        PropertyAccessCode code = accessCodeService.redeem(request.getCode(), jwt.getSubject(), email);
        return DtoMapper.toPropertyAccessCodeRedeemResponse(code);
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("@authz.isAccessCodeCreator(#id, authentication.principal.subject)")
    public PropertyAccessCodeResponse revokeAccessCode(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        return DtoMapper.toPropertyAccessCodeResponse(accessCodeService.revoke(id, jwt.getSubject()));
    }

    @GetMapping("/mine")
    public PageResponsePropertyAccessCodeResponse getMyActiveAccessCodes(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean unpaged,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Le claim 'email' est manquant dans le JWT. Veuillez vérifier la configuration de Keycloak.");
        }

        if (unpaged) {
            return DtoMapper.toAccessCodePage(
                    accessCodeService.findActiveByEmail(email)
            );
        }

        return DtoMapper.toAccessCodePage(
                accessCodeService.findActiveByEmail(email, pageable)
        );
    }
}
