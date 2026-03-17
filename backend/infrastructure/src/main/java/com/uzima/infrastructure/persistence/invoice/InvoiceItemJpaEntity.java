package com.uzima.infrastructure.persistence.invoice;

import com.uzima.domain.payment.model.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entité JPA : Table 'invoice_items'.
 * Appartient à l'agrégat Invoice (cascade ALL depuis InvoiceJpaEntity).
 * <p>
 * TaxRate est stocké en deux colonnes (percentage + label) pour faciliter la reconstitution.
 */
@Entity
@Table(name = "invoice_items")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class InvoiceItemJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "invoice_id", nullable = false, columnDefinition = "uuid")
    private UUID invoiceId;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Currency currency;

    @Column(name = "tax_rate_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRatePercentage;

    @Column(name = "tax_rate_label", nullable = false, length = 30)
    private String taxRateLabel;

    public static InvoiceItemJpaEntity of(
            UUID id, UUID invoiceId, String description, int quantity,
            BigDecimal unitAmount, Currency currency,
            BigDecimal taxRatePercentage, String taxRateLabel
    ) {
        InvoiceItemJpaEntity e = new InvoiceItemJpaEntity();
        e.id                 = id;
        e.invoiceId          = invoiceId;
        e.description        = description;
        e.quantity           = quantity;
        e.unitAmount         = unitAmount;
        e.currency           = currency;
        e.taxRatePercentage  = taxRatePercentage;
        e.taxRateLabel       = taxRateLabel;
        return e;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoiceItemJpaEntity i)) return false;
        return id != null && id.equals(i.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
