-- ============================================================
-- V8 : QR Codes Contextuels Intelligents
-- ============================================================

CREATE TABLE qr_codes (
    id             UUID        NOT NULL PRIMARY KEY,
    owner_id       UUID        NOT NULL REFERENCES users(id),
    type           VARCHAR(30) NOT NULL
        CHECK (type IN (
            'PROFESSIONAL', 'SOCIAL', 'PAYMENT',
            'TEMPORARY_LOCATION', 'EVENT', 'MEDICAL_EMERGENCY'
        )),
    expires_at     TIMESTAMPTZ,
    max_scans      INTEGER     CHECK (max_scans IS NULL OR max_scans >= 1),
    created_at     TIMESTAMPTZ NOT NULL,
    scan_count     INTEGER     NOT NULL DEFAULT 0 CHECK (scan_count >= 0),
    revoked        BOOLEAN     NOT NULL DEFAULT FALSE,
    revoked_at     TIMESTAMPTZ,

    -- Géofencing (F1.4)
    geofence_latitude       DOUBLE PRECISION
        CHECK (geofence_latitude IS NULL OR (geofence_latitude BETWEEN -90 AND 90)),
    geofence_longitude      DOUBLE PRECISION
        CHECK (geofence_longitude IS NULL OR (geofence_longitude BETWEEN -180 AND 180)),
    geofence_radius_meters  INTEGER
        CHECK (geofence_radius_meters IS NULL OR geofence_radius_meters > 0),

    -- Personnalisation (F1.4)
    personalization_condition       VARCHAR(100),
    personalization_target_profile  VARCHAR(100),

    -- Cohérence géofence : soit tous null soit tous renseignés
    CONSTRAINT geofence_consistency CHECK (
        (geofence_latitude IS NULL AND geofence_longitude IS NULL AND geofence_radius_meters IS NULL)
        OR
        (geofence_latitude IS NOT NULL AND geofence_longitude IS NOT NULL AND geofence_radius_meters IS NOT NULL)
    ),

    -- Cohérence personnalisation
    CONSTRAINT personalization_consistency CHECK (
        (personalization_condition IS NULL AND personalization_target_profile IS NULL)
        OR
        (personalization_condition IS NOT NULL AND personalization_target_profile IS NOT NULL)
    )
);

CREATE INDEX idx_qr_codes_owner_id  ON qr_codes (owner_id);
CREATE INDEX idx_qr_codes_type      ON qr_codes (type);
CREATE INDEX idx_qr_codes_active    ON qr_codes (owner_id, revoked, expires_at)
    WHERE revoked = FALSE;
