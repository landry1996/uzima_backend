package com.uzima.infrastructure.persistence.invoice;

import com.uzima.domain.invoice.model.InvoiceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité JPA : Table 'invoices'.
 * Les lignes (InvoiceItem) sont gérées en cascade (aggregate boundary).
 */
@Entity
@Table(
    name = "invoices",
    indexes = {
        @Index(name = "idx_invoices_issuer_id",        columnList = "issuer_id"),
        @Index(name = "idx_invoices_client_id",        columnList = "client_id"),
        @Index(name = "idx_invoices_status",           columnList = "status"),
        @Index(name = "idx_invoices_issuer_status",    columnList = "issuer_id, status"),
        @Index(name = "idx_invoices_created_at",       columnList = "created_at DESC")
    }
)
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class InvoiceJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "issuer_id", nullable = false, columnDefinition = "uuid")
    private UUID issuerId;

    @Column(name = "client_id", nullable = false, columnDefinition = "uuid")
    private UUID clientId;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private List<InvoiceItemJpaEntity> items = new ArrayList<>();

    public static InvoiceJpaEntity of(
            UUID id, UUID issuerId, UUID clientId, LocalDate dueDate,
            InvoiceStatus status, Instant createdAt,
            Instant sentAt, Instant paidAt, Instant cancelledAt,
            List<InvoiceItemJpaEntity> items
    ) {
        InvoiceJpaEntity e = new InvoiceJpaEntity();
        e.id          = id;
        e.issuerId    = issuerId;
        e.clientId    = clientId;
        e.dueDate     = dueDate;
        e.status      = status;
        e.createdAt   = createdAt;
        e.sentAt      = sentAt;
        e.paidAt      = paidAt;
        e.cancelledAt = cancelledAt;
        e.items.addAll(items);
        return e;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoiceJpaEntity i)) return false;
        return id != null && id.equals(i.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
