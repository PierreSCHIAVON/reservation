-- Table des réservations
CREATE TABLE reservations (
    id                  UUID PRIMARY KEY,
    property_id         UUID NOT NULL REFERENCES properties(id),
    tenant_sub          VARCHAR(64) NOT NULL,
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    status              VARCHAR(16) NOT NULL DEFAULT 'PENDING',

    -- Pricing
    unit_price_applied  DECIMAL(10, 2) NOT NULL CHECK (unit_price_applied >= 0),
    total_price         DECIMAL(10, 2) NOT NULL CHECK (total_price >= 0),
    pricing_type        VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    pricing_reason      VARCHAR(255),
    priced_by_sub       VARCHAR(64),

    -- Audit
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_reservations_dates CHECK (end_date > start_date),
    CONSTRAINT chk_reservations_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_reservations_pricing_type CHECK (pricing_type IN ('NORMAL', 'FREE', 'DISCOUNT'))
);

-- Index pour les recherches fréquentes
CREATE INDEX idx_reservations_tenant_sub ON reservations(tenant_sub);
CREATE INDEX idx_reservations_property_id ON reservations(property_id);
CREATE INDEX idx_reservations_dates ON reservations(property_id, start_date, end_date);
CREATE INDEX idx_reservations_pricing_type ON reservations(pricing_type);
CREATE INDEX idx_reservations_status ON reservations(status);
