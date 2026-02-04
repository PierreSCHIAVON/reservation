package com.example.reservation.domain.property;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "property_access_codes",
        indexes = {
                @Index(name = "idx_access_code_lookup", columnList = "code_lookup", unique = true),
                @Index(name = "idx_access_code_email", columnList = "issued_to_email"),
                @Index(name = "idx_access_code_property", columnList = "property_id")
        }
)
public class PropertyAccessCode {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "issued_to_email", nullable = false, length = 255)
    private String issuedToEmail;

    @NotBlank
    @Size(min = 64, max = 64)
    @Column(name = "code_lookup", nullable = false, length = 64, unique = true)
    private String codeLookup;

    @NotBlank
    @Size(max = 100)
    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @NotBlank
    @Size(max = 64)
    @Column(name = "created_by_sub", nullable = false, length = 64)
    private String createdBySub;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @Size(max = 64)
    @Column(name = "redeemed_by_sub", length = 64)
    private String redeemedBySub;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Size(max = 64)
    @Column(name = "revoked_by_sub", length = 64)
    private String revokedBySub;

    // ===== Lifecycle =====

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    // ===== Helpers (lecture seule) =====

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isRedeemed() {
        return redeemedAt != null;
    }

    public boolean isActive() {
        return !isRevoked() && !isExpired() && !isRedeemed();
    }

    public boolean isIssuedTo(String email) {
        return this.issuedToEmail != null && this.issuedToEmail.equalsIgnoreCase(email);
    }

    public boolean isCreatedBy(String userSub) {
        return this.createdBySub != null && this.createdBySub.equals(userSub);
    }
}
