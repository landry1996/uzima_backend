package com.uzima.infrastructure.persistence.invoice;

import com.uzima.domain.invoice.model.Invoice;
import com.uzima.domain.invoice.model.InvoiceId;
import com.uzima.domain.invoice.model.InvoiceItem;
import com.uzima.domain.invoice.model.InvoiceItemId;
import com.uzima.domain.invoice.model.TaxRate;
import com.uzima.domain.payment.model.Money;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.UUID;

/**
 * Mapper : Conversion bidirectionnelle entre Invoice (domaine) et InvoiceJpaEntity (infra).
 * <p>
 * TaxRate est reconstitué à partir de (percentage, label) stockés en base.
 */
public final class InvoiceEntityMapper {

    private InvoiceEntityMapper() {}

    // -------------------------------------------------------------------------
    // Domaine → JPA
    // -------------------------------------------------------------------------

    public static InvoiceJpaEntity toJpaEntity(Invoice invoice) {
        List<InvoiceItemJpaEntity> itemEntities = invoice.items().stream()
                .map(item -> InvoiceItemJpaEntity.of(
                    item.id().value(),
                    invoice.id().value(),
                    item.description(),
                    item.quantity(),
                    item.unitPrice().amount(),
                    item.unitPrice().currency(),
                    item.taxRate().percentage(),
                    item.taxRate().label()
                ))
                .toList();

        return InvoiceJpaEntity.of(
            invoice.id().value(),
            invoice.issuerId().value(),
            invoice.clientId().value(),
            invoice.dueDate(),
            invoice.status(),
            invoice.createdAt(),
            invoice.sentAt().orElse(null),
            invoice.paidAt().orElse(null),
            invoice.cancelledAt().orElse(null),
            itemEntities
        );
    }

    // -------------------------------------------------------------------------
    // JPA → Domaine
    // -------------------------------------------------------------------------

    public static Invoice toDomain(InvoiceJpaEntity entity) {
        List<InvoiceItem> items = entity.getItems().stream()
                .map(i -> InvoiceItem.reconstitute(
                    InvoiceItemId.of(i.getId()),
                    i.getDescription(),
                    i.getQuantity(),
                    Money.of(i.getUnitAmount(), i.getCurrency()),
                    new TaxRate(i.getTaxRatePercentage(), i.getTaxRateLabel())
                ))
                .toList();

        return Invoice.reconstitute(
            InvoiceId.of(entity.getId()),
            UserId.of(entity.getIssuerId()),
            UserId.of(entity.getClientId()),
            entity.getDueDate(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getSentAt(),
            entity.getPaidAt(),
            entity.getCancelledAt(),
            items
        );
    }
}
