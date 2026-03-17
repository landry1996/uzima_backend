-- ============================================================
-- V9 : Tables conversations et conversation_participants
-- ============================================================

CREATE TABLE IF NOT EXISTS conversations (
    id         UUID         PRIMARY KEY,
    type       VARCHAR(10)  NOT NULL CHECK (type IN ('DIRECT', 'GROUP')),
    title      VARCHAR(255),           -- NULL pour les conversations directes
    created_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE IF NOT EXISTS conversation_participants (
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL,
    PRIMARY KEY (conversation_id, user_id)
);

-- Recherche rapide : "toutes les conversations d'un utilisateur"
CREATE INDEX IF NOT EXISTS idx_conv_participants_user_id
    ON conversation_participants (user_id);
