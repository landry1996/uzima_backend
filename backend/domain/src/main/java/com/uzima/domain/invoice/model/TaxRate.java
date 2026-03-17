package com.uzima.domain.invoice.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object : Taux de TVA applicable sur une ligne de facture.
 * <p>
 * Invariants :
 * - percentage ∈ [0, 100]
 * - label non vide
 * <p>
 * Constantes prédéfinies pour les marchés cibles d'Uzima.
 */
public record TaxRate(BigDecimal percentage, String label) {

    public TaxRate {
        Objects.requireNonNull(percentage, "Le taux ne peut pas être nul");
        Objects.requireNonNull(label,      "Le libellé ne peut pas être nul");
        if (label.isBlank()) {
            throw new InvalidTaxRateException("Le libellé du taux ne peut pas être vide");
        }
        if (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new InvalidTaxRateException(
                "Le taux doit être compris entre 0 et 100 : " + percentage
            );
        }
        percentage = percentage.setScale(2, RoundingMode.HALF_EVEN);
    }

    // -------------------------------------------------------------------------
    // Constantes prédéfinies
    // -------------------------------------------------------------------------

    /** Exonéré de TVA (0%). */
    public static final TaxRate EXEMPT         = new TaxRate(BigDecimal.ZERO, "Exonéré");

    /** TVA 18% — Sénégal, Côte d'Ivoire (UEMOA). */
    public static final TaxRate TVA_18         = new TaxRate(new BigDecimal("18"), "TVA 18%");

    /** TVA 19.25% — Cameroun (CEMAC). */
    public static final TaxRate TVA_19_25      = new TaxRate(new BigDecimal("19.25"), "TVA 19.25%");

    /** TVA 20% — France, Maroc. */
    public static final TaxRate TVA_20         = new TaxRate(new BigDecimal("20"), "TVA 20%");

    /** VAT 7.5% — Nigeria. */
    public static final TaxRate VAT_7_5        = new TaxRate(new BigDecimal("7.5"), "VAT 7.5%");

    // -------------------------------------------------------------------------
    // Factory depuis un code string
    // -------------------------------------------------------------------------

    /**
     * Résout un TaxRate depuis son code (EXEMPT, TVA_18, TVA_19_25, TVA_20, VAT_7_5).
     *
     * @throws InvalidTaxRateException si le code est inconnu
     */
    public static TaxRate forCode(String code) {
        Objects.requireNonNull(code, "Le code du taux ne peut pas être nul");
        return switch (code.toUpperCase()) {
            case "EXEMPT"    -> EXEMPT;
            case "TVA_18"    -> TVA_18;
            case "TVA_19_25" -> TVA_19_25;
            case "TVA_20"    -> TVA_20;
            case "VAT_7_5"   -> VAT_7_5;
            default -> throw new InvalidTaxRateException("Code de taux inconnu : " + code);
        };
    }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------

    /**
     * Calcule le montant de taxe sur une base donnée.
     *
     * @param base Montant HT
     * @return Montant de la taxe (base × percentage / 100)
     */
    public BigDecimal applyTo(BigDecimal base) {
        Objects.requireNonNull(base, "La base de calcul ne peut pas être nulle");
        return base.multiply(percentage)
                   .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
    }

    @Override
    public String toString() { return label + " (" + percentage.toPlainString() + "%)"; }

    public static final class InvalidTaxRateException extends RuntimeException {
        public InvalidTaxRateException(String message) { super(message); }
    }
}
