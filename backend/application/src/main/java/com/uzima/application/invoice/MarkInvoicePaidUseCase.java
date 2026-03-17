package com.uzima.application.invoice;

import com.uzima.application.invoice.port.in.MarkInvoicePaidCommand;
import com.uzima.application.invoice.port.out.InvoiceRepositoryPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.domain.invoice.model.Invoice;
import com.uzima.domain.shared.TimeProvider;

import java.util.Objects;

/**
 * Use Case : Marquage d'une facture comme payée (SENT → PAID).
 * <p>
 * Seul l'émetteur (issuerId) peut marquer une facture comme payée.
 */
public final class MarkInvoicePaidUseCase {

    private final InvoiceRepositoryPort invoiceRepository;
    private final TimeProvider          clock;

    public MarkInvoicePaidUseCase(InvoiceRepositoryPort invoiceRepository, TimeProvider clock) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "Le repository de factures est obligatoire");
        this.clock             = Objects.requireNonNull(clock,             "Le fournisseur de temps est obligatoire");
    }

    /**
     * @throws ResourceNotFoundException         si la facture est introuvable
     * @throws UnauthorizedException             si le demandeur n'est pas l'émetteur
     * @throws Invoice.IllegalTransitionException si la facture n'est pas en SENT
     */
    public void execute(MarkInvoicePaidCommand command) {
        Objects.requireNonNull(command, "La commande est obligatoire");

        Invoice invoice = invoiceRepository.findById(command.invoiceId())
                .orElseThrow(() -> ResourceNotFoundException.invoiceNotFound(command.invoiceId()));

        if (!invoice.issuerId().equals(command.requesterId())) {
            throw UnauthorizedException.notInvoiceIssuer(command.invoiceId());
        }

        invoice.markAsPaid(clock);
        invoiceRepository.save(invoice);
    }
}
