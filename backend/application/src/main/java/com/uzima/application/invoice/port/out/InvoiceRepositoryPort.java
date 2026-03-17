package com.uzima.application.invoice.port.out;

import com.uzima.domain.invoice.model.Invoice;
import com.uzima.domain.invoice.model.InvoiceId;
import com.uzima.domain.invoice.model.InvoiceStatus;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Optional;

/** Port OUT : Persistance des factures. */
public interface InvoiceRepositoryPort {

    void save(Invoice invoice);

    Optional<Invoice> findById(InvoiceId id);

    List<Invoice> findByIssuerId(UserId issuerId);

    List<Invoice> findByClientId(UserId clientId);

    List<Invoice> findByIssuerIdAndStatus(UserId issuerId, InvoiceStatus status);
}
