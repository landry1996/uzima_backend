package com.uzima.domain.payment.model;

/**
 * Enum : Devises supportées par Uzima.
 * Priorité Afrique de l'Ouest + devises internationales courantes.
 * XOF : Franc CFA (UEMOA — Sénégal, Côte d'Ivoire, Burkina Faso, Mali, etc.)
 * XAF : Franc CFA (CEMAC — Cameroun, Gabon, Congo, etc.)
 * GHS : Cedi ghanéen
 * NGN : Naira nigérian
 * EUR : Euro
 * USD : Dollar américain
 */
public enum Currency {

    XOF("Franc CFA UEMOA", "FCFA", 0),
    XAF("Franc CFA CEMAC", "FCFA", 0),
    GHS("Cedi ghanéen", "GHS", 2),
    NGN("Naira nigérian", "₦", 2),
    EUR("Euro", "€", 2),
    USD("Dollar américain", "$", 2);

    private final String displayName;
    private final String symbol;
    private final int defaultScale;

    Currency(String displayName, String symbol, int defaultScale) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.defaultScale = defaultScale;
    }

    public String displayName() {
        return displayName;
    }

    public String symbol() {
        return symbol;
    }

    /** Nombre de décimales standard pour cette devise (0 pour XOF/XAF). */
    public int defaultScale() {
        return defaultScale;
    }
}
