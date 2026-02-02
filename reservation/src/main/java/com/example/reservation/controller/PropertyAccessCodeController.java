package com.example.reservation.controller;

import com.example.reservation.domain.property.PropertyAccessCode;
import com.example.reservation.dto.PropertyAccessCodeDto;
import com.example.reservation.service.PropertyAccessCodeService;
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
@RequestMapping("/api/access-codes")
@RequiredArgsConstructor
public class PropertyAccessCodeController {

    private final PropertyAccessCodeService accessCodeService;
    private final PropertyService propertyService;

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("@authz.isPropertyOwner(#propertyId, authentication.name)")
    public List<PropertyAccessCodeDto.Response> getAccessCodesForProperty(@PathVariable UUID propertyId) {
        return accessCodeService.findByProperty(propertyId).stream()
                .map(PropertyAccessCodeDto.Response::from)
                .toList();
    }

    @PostMapping
    @PreAuthorize("@authz.isPropertyOwner(#request.propertyId(), authentication.name)")
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
    public PropertyAccessCodeDto.Response revokeAccessCode(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        return PropertyAccessCodeDto.Response.from(accessCodeService.revoke(id, jwt.getSubject()));
    }

    @GetMapping("/mine")
    public List<PropertyAccessCodeDto.Response> getMyActiveAccessCodes(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            return List.of();
        }

        return accessCodeService.findActiveByEmail(email).stream()
                .map(PropertyAccessCodeDto.Response::from)
                .toList();
    }
}
