-- ============================================================
-- V5 : Facturation — Factures et Lignes de facture
-- ============================================================

-- ------------------------------------------------------------
-- Table : invoices
-- ------------------------------------------------------------
CREATE TABLE invoices (
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    issuer_id    UUID        NOT NULL,
    client_id    UUID        NOT NULL,
    due_date     DATE        NOT NULL,
    status       VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'SENT', 'PAID', 'OVERDUE', 'CANCELLED')),
    created_at   TIMESTAMPTZ NOT NULL,
    sent_at      TIMESTAMPTZ,
    paid_at      TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,

    CONSTRAINT pk_invoices          PRIMARY KEY (id),
    CONSTRAINT fk_invoices_issuer   FOREIGN KEY (issuer_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoices_client   FOREIGN KEY (client_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_invoices_no_self CHECK (issuer_id <> client_id)
);

CREATE INDEX idx_invoices_issuer_id     ON invoices (issuer_id);
CREATE INDEX idx_invoices_client_id     ON invoices (client_id);
CREATE INDEX idx_invoices_status        ON invoices (status);
CREATE INDEX idx_invoices_issuer_status ON invoices (issuer_id, status);
CREATE INDEX idx_invoices_created_at    ON invoices (created_at DESC);

-- ------------------------------------------------------------
-- Table : invoice_items
-- ------------------------------------------------------------
CREATE TABLE invoice_items (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid(),
    invoice_id          UUID          NOT NULL,
    description         VARCHAR(500)  NOT NULL,
    quantity            INT           NOT NULL CHECK (quantity > 0),
    unit_amount         NUMERIC(19,4) NOT NULL CHECK (unit_amount >= 0),
    currency            VARCHAR(10)   NOT NULL,
    tax_rate_percentage NUMERIC(5,2)  NOT NULL CHECK (tax_rate_percentage >= 0),
    tax_rate_label      VARCHAR(30)   NOT NULL,

    CONSTRAINT pk_invoice_items        PRIMARY KEY (id),
    CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE
);

CREATE INDEX idx_invoice_items_invoice_id ON invoice_items (invoice_id);
