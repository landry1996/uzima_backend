-- =============================================================================
-- V1 : Tables fondamentales — Users, Messages, Refresh Tokens
-- Socle du backend Uzima (Sprint 1)
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Table : users
-- ---------------------------------------------------------------------------

CREATE TABLE users (
    id               UUID         NOT NULL,
    phone_number     VARCHAR(20)  NOT NULL,
    country_code     VARCHAR(2)   NOT NULL,
    first_name       VARCHAR(50)  NOT NULL,
    last_name        VARCHAR(50)  NOT NULL,
    avatar_url       TEXT,
    presence_status  VARCHAR(30)  NOT NULL DEFAULT 'OFFLINE',
    is_premium       BOOLEAN      NOT NULL DEFAULT FALSE,
    password_hash    TEXT         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_users             PRIMARY KEY (id),
    CONSTRAINT uq_users_phone       UNIQUE      (phone_number),
    CONSTRAINT ck_users_presence    CHECK       (presence_status IN ('ONLINE','OFFLINE','BUSY','DO_NOT_DISTURB'))
);

CREATE INDEX idx_users_phone_number ON users (phone_number);

COMMENT ON TABLE  users              IS 'Utilisateurs enregistrés sur Uzima';
COMMENT ON COLUMN users.phone_number IS 'Numéro E.164 — identifiant unique de connexion';
COMMENT ON COLUMN users.password_hash IS 'Hash BCrypt du mot de passe — jamais en clair';

-- ---------------------------------------------------------------------------
-- Table : messages
-- (V7 ajoutera les colonnes metadata_*)
-- ---------------------------------------------------------------------------

CREATE TABLE messages (
    id               UUID         NOT NULL,
    conversation_id  UUID         NOT NULL,
    sender_id        UUID         NOT NULL,
    content          VARCHAR(4096) NOT NULL,
    message_type     VARCHAR(30)  NOT NULL DEFAULT 'TEXT',
    sent_at          TIMESTAMPTZ  NOT NULL,
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMPTZ,

    CONSTRAINT pk_messages          PRIMARY KEY (id),
    CONSTRAINT ck_messages_type     CHECK       (message_type IN ('TEXT','VOICE','IMAGE','FILE','PAYMENT_REQUEST','SYSTEM'))
);

CREATE INDEX idx_messages_conversation_id ON messages (conversation_id);
CREATE INDEX idx_messages_sent_at         ON messages (sent_at DESC);
CREATE INDEX idx_messages_sender_id       ON messages (sender_id);

COMMENT ON TABLE  messages IS 'Messages échangés dans les conversations Uzima';

-- ---------------------------------------------------------------------------
-- Table : refresh_tokens
-- ---------------------------------------------------------------------------

CREATE TABLE refresh_tokens (
    token_id      UUID         NOT NULL,
    family_id     UUID         NOT NULL,
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    hashed_value  VARCHAR(64)  NOT NULL,
    issued_at     TIMESTAMPTZ  NOT NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    revoked       BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at    TIMESTAMPTZ,

    CONSTRAINT pk_refresh_tokens          PRIMARY KEY (token_id),
    CONSTRAINT uq_refresh_tokens_hashed   UNIQUE      (hashed_value)
);

CREATE INDEX idx_refresh_tokens_hashed_value ON refresh_tokens (hashed_value);
CREATE INDEX idx_refresh_tokens_family       ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_user         ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires_at   ON refresh_tokens (expires_at);

COMMENT ON TABLE  refresh_tokens              IS 'Refresh tokens JWT avec rotation et révocation par famille';
COMMENT ON COLUMN refresh_tokens.hashed_value IS 'SHA-256 du token brut — le token clair n''est jamais stocké';
COMMENT ON COLUMN refresh_tokens.family_id    IS 'Identifiant de la famille : tous les tokens issus d''une même session partagent le même family_id';
