package com.uzima.infrastructure.persistence.invoice;

import com.uzima.domain.invoice.model.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository pour les factures. */
public interface SpringDataInvoiceRepository extends JpaRepository<InvoiceJpaEntity, UUID> {

    List<InvoiceJpaEntity> findByIssuerId(UUID issuerId);

    List<InvoiceJpaEntity> findByClientId(UUID clientId);

    List<InvoiceJpaEntity> findByIssuerIdAndStatus(UUID issuerId, InvoiceStatus status);
}
