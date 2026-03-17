-- =============================================================================
-- V10 : Table wallets — Portefeuilles internes Uzima
-- Un portefeuille par utilisateur, identifié par owner_id (unique).
-- =============================================================================

CREATE TABLE wallets (
    id         UUID         NOT NULL,
    owner_id   UUID         NOT NULL,
    balance    NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency   VARCHAR(10)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_wallets          PRIMARY KEY (id),
    CONSTRAINT uq_wallets_owner_id UNIQUE      (owner_id),
    CONSTRAINT ck_wallets_balance  CHECK       (balance >= 0)
);

CREATE INDEX idx_wallets_owner_id ON wallets (owner_id);
