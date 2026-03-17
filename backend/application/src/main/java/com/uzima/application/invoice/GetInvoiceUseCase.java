package com.uzima.application.invoice;

import com.uzima.application.invoice.port.out.InvoiceRepositoryPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.domain.invoice.model.Invoice;
import com.uzima.domain.invoice.model.InvoiceId;
import com.uzima.domain.invoice.model.InvoiceStatus;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Objects;

/**
 * Use Case (Query) : Récupération de factures.
 * <p>
 * Un utilisateur peut voir :
 * - ses factures émises (issuerId)
 * - ses factures reçues (clientId)
 */
public final class GetInvoiceUseCase {

    private final InvoiceRepositoryPort invoiceRepository;

    public GetInvoiceUseCase(InvoiceRepositoryPort invoiceRepository) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "Le repository de factures est obligatoire");
    }

    /**
     * Récupère une facture par identifiant.
     * L'utilisateur doit être l'émetteur ou le client.
     *
     * @throws ResourceNotFoundException si introuvable
     * @throws UnauthorizedException     si l'utilisateur n'est ni émetteur ni client
     */
    public Invoice findById(InvoiceId invoiceId, UserId requesterId) {
        Objects.requireNonNull(invoiceId,   "L'identifiant de la facture est obligatoire");
        Objects.requireNonNull(requesterId, "L'identifiant du demandeur est obligatoire");

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> ResourceNotFoundException.invoiceNotFound(invoiceId));

        boolean canAccess = invoice.issuerId().equals(requesterId)
                         || invoice.clientId().equals(requesterId);
        if (!canAccess) {
            throw UnauthorizedException.accessDenied("Invoice", invoiceId);
        }

        return invoice;
    }

    /** Factures émises par l'utilisateur. */
    public List<Invoice> findSent(UserId userId) {
        Objects.requireNonNull(userId, "L'identifiant utilisateur est obligatoire");
        return invoiceRepository.findByIssuerId(userId);
    }

    /** Factures reçues par l'utilisateur. */
    public List<Invoice> findReceived(UserId userId) {
        Objects.requireNonNull(userId, "L'identifiant utilisateur est obligatoire");
        return invoiceRepository.findByClientId(userId);
    }

    /** Factures émises par l'utilisateur filtrées par statut. */
    public List<Invoice> findSentByStatus(UserId userId, InvoiceStatus status) {
        Objects.requireNonNull(userId,  "L'identifiant utilisateur est obligatoire");
        Objects.requireNonNull(status,  "Le statut est obligatoire");
        return invoiceRepository.findByIssuerIdAndStatus(userId, status);
    }
}
