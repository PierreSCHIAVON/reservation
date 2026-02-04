package com.example.reservation.service;

import com.example.reservation.domain.property.Property;
import com.example.reservation.domain.property.PropertyAccessCode;
import com.example.reservation.repository.PropertyAccessCodeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyAccessCodeService {

    private final PropertyAccessCodeRepository accessCodeRepository;
    private final PropertyService propertyService;
    private final PasswordEncoder passwordEncoder;
    private static final SecureRandom secureRandom = new SecureRandom();

    public PropertyAccessCode findById(UUID id) {
        return accessCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Access code not found: " + id));
    }

    public List<PropertyAccessCode> findByProperty(UUID propertyId) {
        return accessCodeRepository.findByPropertyId(propertyId);
    }

    public List<PropertyAccessCode> findActiveByProperty(UUID propertyId) {
        return accessCodeRepository.findActiveByPropertyId(propertyId);
    }

    public List<PropertyAccessCode> findActiveByEmail(String email) {
        return accessCodeRepository.findActiveByEmail(email);
    }

    // ===== Paginated methods (two-query pattern) =====

    public Page<PropertyAccessCode> findByProperty(UUID propertyId, Pageable pageable) {
        Page<UUID> idsPage = accessCodeRepository.findIdsByPropertyId(propertyId, pageable);
        return fetchWithProperty(idsPage);
    }

    public Page<PropertyAccessCode> findActiveByEmail(String email, Pageable pageable) {
        Page<UUID> idsPage = accessCodeRepository.findActiveIdsByEmail(email, pageable);
        return fetchWithProperty(idsPage);
    }

    /**
     * Two-query pattern: fetch entities with JOIN FETCH maintaining the order from the ID page.
     */
    private Page<PropertyAccessCode> fetchWithProperty(Page<UUID> idsPage) {
        if (idsPage.isEmpty()) {
            return new PageImpl<>(List.of(), idsPage.getPageable(), idsPage.getTotalElements());
        }

        List<UUID> ids = idsPage.getContent();
        List<PropertyAccessCode> codes = accessCodeRepository.findByIdsWithProperty(ids);

        // Maintain original order from paginated IDs
        Map<UUID, PropertyAccessCode> codeMap = codes.stream()
                .collect(Collectors.toMap(PropertyAccessCode::getId, Function.identity()));
        List<PropertyAccessCode> orderedCodes = ids.stream()
                .map(codeMap::get)
                .toList();

        return new PageImpl<>(orderedCodes, idsPage.getPageable(), idsPage.getTotalElements());
    }

    @Transactional
    public PropertyAccessCodeResult create(UUID propertyId, String issuedToEmail, String createdBySub, Instant expiresAt) {
        Property property = propertyService.findById(propertyId);

        // Générer un code aléatoire
        String rawCode = generateSecureCode();

        // Créer le lookup (SHA-256) pour recherche rapide
        String codeLookup = sha256(rawCode);

        // Créer le hash (BCrypt) pour validation sécurisée
        String codeHash = passwordEncoder.encode(rawCode);

        PropertyAccessCode accessCode = PropertyAccessCode.builder()
                .property(property)
                .issuedToEmail(issuedToEmail.toLowerCase())
                .codeLookup(codeLookup)
                .codeHash(codeHash)
                .createdBySub(createdBySub)
                .expiresAt(expiresAt)
                .build();

        PropertyAccessCode saved = accessCodeRepository.save(accessCode);

        // Retourner le code brut (à envoyer à l'utilisateur) + l'entité
        return new PropertyAccessCodeResult(saved, rawCode);
    }

    @Transactional
    public PropertyAccessCode redeem(String rawCode, String userSub, String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email manquant dans le token");
        }
        String codeLookup = sha256(rawCode);

        PropertyAccessCode accessCode = accessCodeRepository.findByCodeLookup(codeLookup)
                .orElseThrow(() -> new EntityNotFoundException("Code d'accès invalide"));

        if (!accessCode.isIssuedTo(email)) {
            throw new EntityNotFoundException("Code d'accès invalide");
        }

        if (!accessCode.isActive()) {
            throw new IllegalStateException("Ce code n'est plus actif");
        }

        // Vérifier le hash BCrypt
        if (!passwordEncoder.matches(rawCode, accessCode.getCodeHash())) {
            throw new IllegalStateException("Code d'accès invalide");
        }

        accessCode.setRedeemedAt(Instant.now());
        accessCode.setRedeemedBySub(userSub);

        return accessCodeRepository.save(accessCode);
    }

    @Transactional
    public PropertyAccessCode revoke(UUID id, String revokedBySub) {
        PropertyAccessCode accessCode = findById(id);

        if (accessCode.isRevoked()) {
            throw new IllegalStateException("Ce code est déjà révoqué");
        }

        accessCode.setRevokedAt(Instant.now());
        accessCode.setRevokedBySub(revokedBySub);

        return accessCodeRepository.save(accessCode);
    }

    public boolean validateCode(String rawCode) {
        String codeLookup = sha256(rawCode);

        return accessCodeRepository.findByCodeLookup(codeLookup)
                .filter(PropertyAccessCode::isActive)
                .filter(code -> passwordEncoder.matches(rawCode, code.getCodeHash()))
                .isPresent();
    }

    public boolean isCreator(UUID accessCodeId, String userSub) {
        PropertyAccessCode accessCode = findById(accessCodeId);
        return accessCode.isCreatedBy(userSub);
    }

    private String generateSecureCode() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public record PropertyAccessCodeResult(PropertyAccessCode accessCode, String rawCode) {}
}
