package com.uzima.domain.invoice.model;

import com.uzima.domain.payment.model.Money;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entity : Ligne d'une facture.
 * <p>
 * Appartient au cycle de vie de Invoice (aggregate boundary).
 * Constructeur package-private — instancié uniquement via Invoice.addItem().
 * <p>
 * Calculs :
 *   subtotal()  = quantity × unitPrice
 *   taxAmount() = subtotal × taxRate.percentage / 100
 *   total()     = subtotal + taxAmount
 */
public final class InvoiceItem {

    private final InvoiceItemId id;
    private final String        description;
    private final int           quantity;
    private final Money         unitPrice;
    private final TaxRate       taxRate;

    // -------------------------------------------------------------------------
    // Constructeur package-private
    // -------------------------------------------------------------------------

    InvoiceItem(InvoiceItemId id, String description, int quantity, Money unitPrice, TaxRate taxRate) {
        this.id          = Objects.requireNonNull(id,          "L'identifiant est obligatoire");
        this.unitPrice   = Objects.requireNonNull(unitPrice,   "Le prix unitaire est obligatoire");
        this.taxRate     = Objects.requireNonNull(taxRate,     "Le taux de TVA est obligatoire");

        if (description == null || description.isBlank()) {
            throw new InvalidItemDescriptionException("La description de la ligne est obligatoire");
        }
        if (quantity <= 0) {
            throw new InvalidQuantityException("La quantité doit être strictement positive : " + quantity);
        }
        this.description = description.strip();
        this.quantity    = quantity;
    }

    // -------------------------------------------------------------------------
    // Factory : reconstitute()
    // -------------------------------------------------------------------------

    /** Reconstitution depuis la persistance — appelé par l'adaptateur infrastructure. */
    public static InvoiceItem reconstitute(InvoiceItemId id, String description,
                                            int quantity, Money unitPrice, TaxRate taxRate) {
        return new InvoiceItem(id, description, quantity, unitPrice, taxRate);
    }

    // -------------------------------------------------------------------------
    // Calculs
    // -------------------------------------------------------------------------

    /** Montant HT : quantity × unitPrice. */
    public Money subtotal() {
        BigDecimal qty = BigDecimal.valueOf(quantity);
        return unitPrice.multiply(qty);
    }

    /** Montant de taxe : subtotal × taxRate%. */
    public Money taxAmount() {
        BigDecimal taxValue = taxRate.applyTo(subtotal().amount());
        return Money.of(taxValue, unitPrice.currency());
    }

    /** Montant TTC : subtotal + taxAmount. */
    public Money total() {
        return subtotal().add(taxAmount());
    }

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    public InvoiceItemId id()          { return id; }
    public String        description() { return description; }
    public int           quantity()    { return quantity; }
    public Money         unitPrice()   { return unitPrice; }
    public TaxRate       taxRate()     { return taxRate; }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoiceItem i)) return false;
        return id.equals(i.id);
    }

    @Override public int hashCode() { return id.hashCode(); }

    // -------------------------------------------------------------------------
    // Exceptions
    // -------------------------------------------------------------------------

    public static final class InvalidItemDescriptionException extends RuntimeException {
        public InvalidItemDescriptionException(String message) { super(message); }
    }

    public static final class InvalidQuantityException extends RuntimeException {
        public InvalidQuantityException(String message) { super(message); }
    }
}
