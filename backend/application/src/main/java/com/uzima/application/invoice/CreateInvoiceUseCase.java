package com.uzima.application.invoice;

import com.uzima.application.invoice.port.in.CreateInvoiceCommand;
import com.uzima.application.invoice.port.out.InvoiceRepositoryPort;
import com.uzima.domain.invoice.model.Invoice;
import com.uzima.domain.invoice.model.InvoiceId;
import com.uzima.domain.payment.model.Money;
import com.uzima.domain.shared.TimeProvider;

import java.util.Objects;

/**
 * Use Case : Création d'une facture avec ses lignes.
 * <p>
 * Orchestration :
 * 1. Créer la facture via Invoice.create() — vérifie self-invoicing
 * 2. Ajouter chaque ligne via addItem() — vérifie cohérence devise
 * 3. Persister la facture
 * 4. Retourner l'InvoiceId
 */
public final class CreateInvoiceUseCase {

    private final InvoiceRepositoryPort invoiceRepository;
    private final TimeProvider          clock;

    public CreateInvoiceUseCase(InvoiceRepositoryPort invoiceRepository, TimeProvider clock) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "Le repository de factures est obligatoire");
        this.clock             = Objects.requireNonNull(clock,             "Le fournisseur de temps est obligatoire");
    }

    /**
     * @return L'identifiant de la facture créée
     * @throws Invoice.SelfInvoicingException     si issuerId == clientId
     * @throws Invoice.CurrencyMismatchException  si les lignes n'ont pas la même devise
     */
    public InvoiceId execute(CreateInvoiceCommand command) {
        Objects.requireNonNull(command, "La commande de création est obligatoire");

        Invoice invoice = Invoice.create(
            command.issuerId(),
            command.clientId(),
            command.dueDate(),
            clock
        );

        for (CreateInvoiceCommand.Item item : command.items()) {
            Money unitPrice = Money.of(item.unitAmount(), item.currency());
            invoice.addItem(item.description(), item.quantity(), unitPrice, item.taxRate());
        }

        invoiceRepository.save(invoice);
        return invoice.id();
    }
}
