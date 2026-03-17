package com.uzima.domain.invoice.model;

/**
 * Enum : États du cycle de vie d'une facture.
 * <p>
 * DRAFT    → SENT (send())
 * SENT     → PAID (markAsPaid())
 * SENT     → OVERDUE (tâche planifiée, pas de méthode explicite)
 * DRAFT/SENT/OVERDUE → CANCELLED (cancel())
 * <p>
 * États terminaux : PAID, CANCELLED
 */
public enum InvoiceStatus {

    DRAFT("Brouillon"),
    SENT("Envoyée"),
    PAID("Payée"),
    OVERDUE("En retard"),
    CANCELLED("Annulée");

    private final String displayName;

    InvoiceStatus(String displayName) { this.displayName = displayName; }

    public String displayName() { return displayName; }

    public boolean isTerminal() { return this == PAID || this == CANCELLED; }

    public boolean isCancellable() { return this != PAID && this != CANCELLED; }
}
