-- Migration V2 : Création de la table transactions
-- Domaine Payment — Sprint 1-2
-- Date : 2026-03-12

CREATE TABLE transactions
(
    id             UUID         NOT NULL,
    sender_id      UUID         NOT NULL,
    recipient_id   UUID         NOT NULL,
    amount         NUMERIC(19, 4) NOT NULL,
    currency       VARCHAR(10)  NOT NULL,
    method         VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    description    VARCHAR(255),
    external_id    VARCHAR(100),
    failure_reason VARCHAR(500),
    initiated_at   TIMESTAMPTZ  NOT NULL,
    completed_at   TIMESTAMPTZ,
    failed_at      TIMESTAMPTZ,
    cancelled_at   TIMESTAMPTZ,

    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT chk_transactions_amount CHECK (amount >= 0),
    CONSTRAINT chk_transactions_self_payment CHECK (sender_id <> recipient_id),
    CONSTRAINT chk_transactions_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_transactions_method CHECK (method IN ('MOBILE_MONEY', 'CARD', 'CRYPTO', 'WALLET')),
    CONSTRAINT chk_transactions_currency CHECK (currency IN ('XOF', 'XAF', 'GHS', 'NGN', 'EUR', 'USD'))
);

-- Index pour les requêtes courantes
CREATE INDEX idx_transactions_sender_id
    ON transactions (sender_id);

CREATE INDEX idx_transactions_recipient_id
    ON transactions (recipient_id);

CREATE INDEX idx_transactions_status
    ON transactions (status);

CREATE INDEX idx_transactions_initiated_at
    ON transactions (initiated_at DESC);

-- Index composite pour l'historique paginé d'un utilisateur (sender)
CREATE INDEX idx_transactions_sender_date
    ON transactions (sender_id, initiated_at DESC);

-- Index composite pour l'historique paginé d'un utilisateur (recipient)
CREATE INDEX idx_transactions_recipient_date
    ON transactions (recipient_id, initiated_at DESC);

COMMENT ON TABLE transactions IS 'Transactions de paiement entre utilisateurs Uzima (Sprint 1-2)';
COMMENT ON COLUMN transactions.external_id IS 'Identifiant de la transaction côté gateway externe (Orange Money, Stripe, etc.)';
COMMENT ON COLUMN transactions.failure_reason IS 'Raison de l''échec retournée par la gateway externe';
