package com.example.reservation.domain.property;

import com.example.reservation.domain.reservation.Reservation;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "properties", indexes = {
        @Index(name = "idx_properties_owner_sub", columnList = "owner_sub"),
        @Index(name = "idx_properties_status", columnList = "status")
})
public class Property {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @NotBlank
    @Size(max = 64)
    @Column(name = "owner_sub", nullable = false, length = 64)
    private String ownerSub;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String title;

    @NotBlank
    @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String description;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String city;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PropertyStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ===== Relations =====

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PropertyAccessCode> accessCodes = new ArrayList<>();

    // ===== Lifecycle =====

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = PropertyStatus.ACTIVE;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ===== Helpers (lecture seule) =====

    public boolean isBookable() {
        return this.status == PropertyStatus.ACTIVE;
    }

    public boolean isOwnedBy(String userSub) {
        return this.ownerSub != null && this.ownerSub.equals(userSub);
    }

    public List<Reservation> getActiveReservations() {
        return reservations.stream()
                .filter(Reservation::isActive)
                .toList();
    }

    public boolean hasOverlap(LocalDate start, LocalDate end) {
        return reservations.stream()
                .filter(Reservation::isActive)
                .anyMatch(r -> !r.getEndDate().isBefore(start) && !r.getStartDate().isAfter(end));
    }
}
