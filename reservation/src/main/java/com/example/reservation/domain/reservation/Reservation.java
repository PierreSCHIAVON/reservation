package com.example.reservation.domain.reservation;

import com.example.reservation.domain.property.Property;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "reservations",
        indexes = {
                @Index(name = "idx_reservations_tenant_sub", columnList = "tenant_sub"),
                @Index(name = "idx_reservations_property_id", columnList = "property_id"),
                @Index(name = "idx_reservations_dates", columnList = "property_id,start_date,end_date"),
                @Index(name = "idx_reservations_pricing_type", columnList = "pricing_type")
        }
)
public class Reservation {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @NotBlank
    @Size(max = 64)
    @Column(name = "tenant_sub", nullable = false, length = 64)
    private String tenantSub;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReservationStatus status;

    // ===== Pricing =====

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "unit_price_applied", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceApplied;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_type", nullable = false, length = 16)
    private PricingType pricingType;

    @Size(max = 255)
    @Column(name = "pricing_reason", length = 255)
    private String pricingReason;

    @Size(max = 64)
    @Column(name = "priced_by_sub", length = 64)
    private String pricedBySub;

    // ===== Audit =====

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ===== Validation =====

    @AssertTrue(message = "La date de fin doit être après la date de début")
    private boolean isEndDateAfterStartDate() {
        return endDate == null || startDate == null || endDate.isAfter(startDate);
    }

    // ===== Lifecycle =====

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = ReservationStatus.PENDING;
        }

        if (this.pricingType == null) {
            this.pricingType = PricingType.NORMAL;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ===== Helpers (lecture seule) =====

    public long getNights() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    public boolean isActive() {
        return this.status == ReservationStatus.PENDING
                || this.status == ReservationStatus.CONFIRMED;
    }

    public boolean isPending() {
        return this.status == ReservationStatus.PENDING;
    }

    public boolean isConfirmed() {
        return this.status == ReservationStatus.CONFIRMED;
    }

    public boolean isCancelled() {
        return this.status == ReservationStatus.CANCELLED;
    }

    public boolean isCompleted() {
        return this.status == ReservationStatus.COMPLETED;
    }

    public boolean isTenant(String userSub) {
        return this.tenantSub != null && this.tenantSub.equals(userSub);
    }

    public boolean overlapsWith(LocalDate start, LocalDate end) {
        return !this.endDate.isBefore(start) && !this.startDate.isAfter(end);
    }
}
