package com.uzima.application.invoice;

import com.uzima.application.invoice.port.in.CancelInvoiceCommand;
import com.uzima.application.invoice.port.out.InvoiceRepositoryPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.domain.invoice.model.Invoice;
import com.uzima.domain.shared.TimeProvider;

import java.util.Objects;

/**
 * Use Case : Annulation d'une facture.
 *
 * Seul l'émetteur (issuerId) peut annuler sa facture (sauf si PAID ou déjà CANCELLED).
 */
public final class CancelInvoiceUseCase {

    private final InvoiceRepositoryPort invoiceRepository;
    private final TimeProvider          clock;

    public CancelInvoiceUseCase(InvoiceRepositoryPort invoiceRepository, TimeProvider clock) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "Le repository de factures est obligatoire");
        this.clock             = Objects.requireNonNull(clock,             "Le fournisseur de temps est obligatoire");
    }

    /**
     * @throws ResourceNotFoundException         si la facture est introuvable
     * @throws UnauthorizedException             si le demandeur n'est pas l'émetteur
     * @throws Invoice.IllegalTransitionException si la facture est dans un état terminal non annulable
     */
    public void execute(CancelInvoiceCommand command) {
        Objects.requireNonNull(command, "La commande est obligatoire");

        Invoice invoice = invoiceRepository.findById(command.invoiceId())
                .orElseThrow(() -> ResourceNotFoundException.invoiceNotFound(command.invoiceId()));

        if (!invoice.issuerId().equals(command.requesterId())) {
            throw UnauthorizedException.notInvoiceIssuer(command.invoiceId());
        }

        invoice.cancel(clock);
        invoiceRepository.save(invoice);
    }
}
