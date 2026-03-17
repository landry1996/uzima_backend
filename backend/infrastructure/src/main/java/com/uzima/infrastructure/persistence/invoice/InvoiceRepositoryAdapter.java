package com.uzima.infrastructure.persistence.invoice;

import com.uzima.application.invoice.port.out.InvoiceRepositoryPort;
import com.uzima.domain.invoice.model.Invoice;
import com.uzima.domain.invoice.model.InvoiceId;
import com.uzima.domain.invoice.model.InvoiceStatus;
import com.uzima.domain.user.model.UserId;
import com.uzima.infrastructure.shared.exception.DatabaseException;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptateur : Implémente InvoiceRepositoryPort avec JPA.
 */
public final class InvoiceRepositoryAdapter implements InvoiceRepositoryPort {

    private final SpringDataInvoiceRepository jpaRepository;

    public InvoiceRepositoryAdapter(SpringDataInvoiceRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository);
    }

    @Override
    public void save(Invoice invoice) {
        try {
            jpaRepository.save(InvoiceEntityMapper.toJpaEntity(invoice));
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la sauvegarde de la facture", ex);
        }
    }

    @Override
    public Optional<Invoice> findById(InvoiceId id) {
        try {
            return jpaRepository.findById(id.value())
                    .map(InvoiceEntityMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la recherche de la facture", ex);
        }
    }

    @Override
    public List<Invoice> findByIssuerId(UserId issuerId) {
        try {
            return jpaRepository.findByIssuerId(issuerId.value()).stream()
                    .map(InvoiceEntityMapper::toDomain)
                    .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la récupération des factures émises", ex);
        }
    }

    @Override
    public List<Invoice> findByClientId(UserId clientId) {
        try {
            return jpaRepository.findByClientId(clientId.value()).stream()
                    .map(InvoiceEntityMapper::toDomain)
                    .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la récupération des factures reçues", ex);
        }
    }

    @Override
    public List<Invoice> findByIssuerIdAndStatus(UserId issuerId, InvoiceStatus status) {
        try {
            return jpaRepository.findByIssuerIdAndStatus(issuerId.value(), status).stream()
                    .map(InvoiceEntityMapper::toDomain)
                    .toList();
        } catch (DataAccessException ex) {
            throw new DatabaseException("Erreur lors de la récupération des factures par statut", ex);
        }
    }
}
