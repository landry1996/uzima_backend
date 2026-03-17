package com.uzima.application.invoice;

import com.uzima.application.invoice.port.in.SendInvoiceCommand;
import com.uzima.application.invoice.port.out.InvoiceRepositoryPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.domain.invoice.model.Invoice;

import java.util.Objects;

import com.uzima.domain.shared.TimeProvider;

/**
 * Use Case : Envoi d'une facture (DRAFT → SENT).
 * <p>
 * Seul l'émetteur (issuerId) peut envoyer sa propre facture.
 */
public final class SendInvoiceUseCase {

    private final InvoiceRepositoryPort invoiceRepository;
    private final TimeProvider          clock;

    public SendInvoiceUseCase(InvoiceRepositoryPort invoiceRepository, TimeProvider clock) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "Le repository de factures est obligatoire");
        this.clock             = Objects.requireNonNull(clock,             "Le fournisseur de temps est obligatoire");
    }

    /**
     * @throws ResourceNotFoundException          si la facture est introuvable
     * @throws UnauthorizedException              si le demandeur n'est pas l'émetteur
     * @throws Invoice.InvoiceCannotBeSentException si la facture est vide ou montant nul
     * @throws Invoice.IllegalTransitionException  si la facture n'est pas en DRAFT
     */
    public void execute(SendInvoiceCommand command) {
        Objects.requireNonNull(command, "La commande est obligatoire");

        Invoice invoice = invoiceRepository.findById(command.invoiceId())
                .orElseThrow(() -> ResourceNotFoundException.invoiceNotFound(command.invoiceId()));

        if (!invoice.issuerId().equals(command.requesterId())) {
            throw UnauthorizedException.notInvoiceIssuer(command.invoiceId());
        }

        invoice.send(clock);
        invoiceRepository.save(invoice);
    }
}
