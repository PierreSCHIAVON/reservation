-- Table des codes d'accès aux propriétés (invitations)
CREATE TABLE property_access_codes (
    id              UUID PRIMARY KEY,
    property_id     UUID NOT NULL REFERENCES properties(id),
    issued_to_email VARCHAR(255) NOT NULL,
    code_lookup     VARCHAR(64) NOT NULL UNIQUE,  -- SHA-256 hex pour lookup rapide
    code_hash       VARCHAR(100) NOT NULL,      -- BCrypt hash pour validation
    created_by_sub  VARCHAR(64) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP,                  -- NULL = jamais
    redeemed_at     TIMESTAMP,
    redeemed_by_sub VARCHAR(64),
    revoked_at      TIMESTAMP,
    revoked_by_sub  VARCHAR(64)
);

-- Index pour les recherches fréquentes
CREATE INDEX idx_access_code_lookup ON property_access_codes(code_lookup);
CREATE INDEX idx_access_code_email ON property_access_codes(LOWER(issued_to_email));
CREATE INDEX idx_access_code_property ON property_access_codes(property_id);
CREATE INDEX idx_access_code_created_by ON property_access_codes(created_by_sub);
