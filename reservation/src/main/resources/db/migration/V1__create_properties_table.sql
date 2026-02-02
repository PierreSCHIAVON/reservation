-- Table des propriétés (biens immobiliers)
CREATE TABLE properties (
    id              UUID PRIMARY KEY,
    owner_sub       VARCHAR(64) NOT NULL,
    title           VARCHAR(120) NOT NULL,
    description     VARCHAR(2000) NOT NULL,
    city            VARCHAR(120) NOT NULL,
    price_per_night DECIMAL(10, 2) NOT NULL CHECK (price_per_night > 0),
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_properties_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- Index pour les recherches fréquentes
CREATE INDEX idx_properties_owner_sub ON properties(owner_sub);
CREATE INDEX idx_properties_status ON properties(status);
CREATE INDEX idx_properties_city ON properties(LOWER(city));
