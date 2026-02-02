package com.example.reservation.controller;

import com.example.reservation.domain.property.PropertyAccessCode;
import com.example.reservation.dto.PropertyAccessCodeDto;
import com.example.reservation.service.PropertyAccessCodeService;
import com.example.reservation.service.PropertyService;
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
@RequestMapping("/api/access-codes")
@RequiredArgsConstructor
public class PropertyAccessCodeController {

    private final PropertyAccessCodeService accessCodeService;
    private final PropertyService propertyService;

    @GetMapping("/property/{propertyId}")
    public List<PropertyAccessCodeDto.Response> getAccessCodesForProperty(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID propertyId
    ) {
        // Seul le propriétaire peut voir les codes
        if (!propertyService.isOwner(propertyId, jwt.getSubject())) {
            throw new ForbiddenException("Vous n'êtes pas propriétaire de ce bien");
        }

        return accessCodeService.findByProperty(propertyId).stream()
                .map(PropertyAccessCodeDto.Response::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<PropertyAccessCodeDto.CreateResponse> createAccessCode(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PropertyAccessCodeDto.CreateRequest request
    ) {
        // Seul le propriétaire peut créer des codes
        if (!propertyService.isOwner(request.propertyId(), jwt.getSubject())) {
            throw new ForbiddenException("Vous n'êtes pas propriétaire de ce bien");
        }

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
    public PropertyAccessCodeDto.Response revokeAccessCode(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id
    ) {
        // Seul le créateur peut révoquer
        if (!accessCodeService.isCreator(id, jwt.getSubject())) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à révoquer ce code");
        }

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
