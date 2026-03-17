package com.uzima.domain.invoice.model;

import com.uzima.domain.payment.model.Currency;
import com.uzima.domain.payment.model.Money;
import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aggregate Root : Facture Uzima.
 * <p>
 * Cycle de vie :
 *   DRAFT → SENT (send()) — total > 0 requis
 *   SENT → PAID (markAsPaid()) — terminal
 *   SENT → OVERDUE (tâche planifiée, pas de méthode domaine)
 *   DRAFT/SENT/OVERDUE → CANCELLED (cancel())
 * <p>
 * Invariants :
 * - L'émetteur ne peut pas être le client (auto-facturation interdite)
 * - Impossible d'ajouter des lignes après l'envoi
 * - Le total doit être > 0 pour envoyer
 * - Toutes les lignes doivent être dans la même devise
 */
public final class Invoice {

    private final InvoiceId   id;
    private final UserId      issuerId;
    private final UserId      clientId;
    private final LocalDate   dueDate;
    private       InvoiceStatus status;
    private final Instant     createdAt;
    private       Instant     sentAt;
    private       Instant     paidAt;
    private       Instant     cancelledAt;

    private final List<InvoiceItem> items        = new ArrayList<>();
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructeur privé
    // -------------------------------------------------------------------------

    private Invoice(InvoiceId id, UserId issuerId, UserId clientId,
                    LocalDate dueDate, InvoiceStatus status, Instant createdAt,
                    Instant sentAt, Instant paidAt, Instant cancelledAt) {
        this.id          = Objects.requireNonNull(id,        "L'identifiant est obligatoire");
        this.issuerId    = Objects.requireNonNull(issuerId,  "L'émetteur est obligatoire");
        this.clientId    = Objects.requireNonNull(clientId,  "Le client est obligatoire");
        this.dueDate     = Objects.requireNonNull(dueDate,   "La date d'échéance est obligatoire");
        this.status      = Objects.requireNonNull(status,    "Le statut est obligatoire");
        this.createdAt   = Objects.requireNonNull(createdAt, "La date de création est obligatoire");
        if (issuerId.equals(clientId)) {
            throw new SelfInvoicingException("L'émetteur et le client ne peuvent pas être identiques");
        }
        this.sentAt      = sentAt;
        this.paidAt      = paidAt;
        this.cancelledAt = cancelledAt;
    }

    // -------------------------------------------------------------------------
    // Factory : create()
    // -------------------------------------------------------------------------

    /**
     * Crée une nouvelle facture en état DRAFT.
     *
     * @throws SelfInvoicingException si issuerId == clientId
     */
    public static Invoice create(UserId issuerId, UserId clientId, LocalDate dueDate, TimeProvider clock) {
        Objects.requireNonNull(issuerId, "L'émetteur est obligatoire");
        Objects.requireNonNull(clientId, "Le client est obligatoire");
        Objects.requireNonNull(dueDate,  "La date d'échéance est obligatoire");
        Objects.requireNonNull(clock,    "Le fournisseur de temps est obligatoire");

        return new Invoice(
            InvoiceId.generate(), issuerId, clientId, dueDate,
            InvoiceStatus.DRAFT, clock.now(), null, null, null
        );
    }

    // -------------------------------------------------------------------------
    // Factory : reconstitute()
    // -------------------------------------------------------------------------

    public static Invoice reconstitute(
            InvoiceId id, UserId issuerId, UserId clientId, LocalDate dueDate,
            InvoiceStatus status, Instant createdAt, Instant sentAt, Instant paidAt,
            Instant cancelledAt, List<InvoiceItem> items
    ) {
        Invoice invoice = new Invoice(id, issuerId, clientId, dueDate, status,
                                      createdAt, sentAt, paidAt, cancelledAt);
        invoice.items.addAll(items);
        return invoice;
    }

    // -------------------------------------------------------------------------
    // Comportements métier
    // -------------------------------------------------------------------------

    /**
     * Ajoute une ligne à la facture.
     *
     * @throws InvoiceAlreadySentException  si la facture n'est plus en DRAFT
     * @throws CurrencyMismatchException    si la devise diffère des lignes existantes
     */
    public void addItem(String description, int quantity, Money unitPrice, TaxRate taxRate) {
        Objects.requireNonNull(unitPrice, "Le prix unitaire est obligatoire");
        Objects.requireNonNull(taxRate,   "Le taux de TVA est obligatoire");

        if (status != InvoiceStatus.DRAFT) {
            throw new InvoiceAlreadySentException(
                "Impossible d'ajouter une ligne à une facture en état " + status.displayName()
            );
        }
        if (!items.isEmpty()) {
            Currency existingCurrency = items.getFirst().unitPrice().currency();
            if (!unitPrice.currency().equals(existingCurrency)) {
                throw new CurrencyMismatchException(
                    "Toutes les lignes doivent être dans la même devise : "
                    + existingCurrency + " ≠ " + unitPrice.currency()
                );
            }
        }
        items.add(new InvoiceItem(InvoiceItemId.generate(), description, quantity, unitPrice, taxRate));
    }

    /**
     * Envoie la facture au client (DRAFT → SENT).
     *
     * @throws InvoiceCannotBeSentException si la facture est vide ou le total est nul
     * @throws IllegalTransitionException   si la facture n'est pas en DRAFT
     */
    public void send(TimeProvider clock) {
        Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
        requireStatus(InvoiceStatus.DRAFT, "envoyer");

        if (items.isEmpty()) {
            throw new InvoiceCannotBeSentException("Impossible d'envoyer une facture sans lignes");
        }
        if (!total().isPositive()) {
            throw new InvoiceCannotBeSentException("Impossible d'envoyer une facture dont le total est nul");
        }

        this.status = InvoiceStatus.SENT;
        this.sentAt = clock.now();
        domainEvents.add(new InvoiceSentEvent(id, issuerId, clientId, total(), sentAt));
    }

    /**
     * Marque la facture comme payée (SENT → PAID).
     *
     * @throws IllegalTransitionException si la facture n'est pas en SENT
     */
    public void markAsPaid(TimeProvider clock) {
        Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
        requireStatus(InvoiceStatus.SENT, "marquer comme payée");

        this.status = InvoiceStatus.PAID;
        this.paidAt = clock.now();
        domainEvents.add(new InvoicePaidEvent(id, issuerId, clientId, total(), paidAt));
    }

    /**
     * Annule la facture (tout état sauf PAID).
     *
     * @throws IllegalTransitionException si la facture est déjà payée ou annulée
     */
    public void cancel(TimeProvider clock) {
        Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
        if (!status.isCancellable()) {
            throw new IllegalTransitionException(
                "Impossible d'annuler une facture en état " + status.displayName()
                + (status.isTerminal() ? " [état terminal]" : "")
            );
        }
        this.status      = InvoiceStatus.CANCELLED;
        this.cancelledAt = clock.now();
    }

    // -------------------------------------------------------------------------
    // Calculs financiers
    // -------------------------------------------------------------------------

    /**
     * Montant HT total (somme des subtotaux de toutes les lignes).
     * Retourne Money.zero si la facture est vide.
     */
    public Money subtotal() {
        if (items.isEmpty()) return Money.zero(defaultCurrency());
        return items.stream()
                .map(InvoiceItem::subtotal)
                .reduce(Money::add)
                .orElse(Money.zero(defaultCurrency()));
    }

    /**
     * Montant total de TVA (somme des taxAmount de toutes les lignes).
     */
    public Money taxAmount() {
        if (items.isEmpty()) return Money.zero(defaultCurrency());
        return items.stream()
                .map(InvoiceItem::taxAmount)
                .reduce(Money::add)
                .orElse(Money.zero(defaultCurrency()));
    }

    /**
     * Montant TTC total (subtotal + taxAmount).
     */
    public Money total() {
        return subtotal().add(taxAmount());
    }

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    public InvoiceId     id()          { return id; }
    public UserId        issuerId()    { return issuerId; }
    public UserId        clientId()    { return clientId; }
    public LocalDate     dueDate()     { return dueDate; }
    public InvoiceStatus status()      { return status; }
    public Instant       createdAt()   { return createdAt; }
    public int           itemCount()   { return items.size(); }

    public List<InvoiceItem> items()       { return Collections.unmodifiableList(items); }
    public Optional<Instant> sentAt()      { return Optional.ofNullable(sentAt); }
    public Optional<Instant> paidAt()      { return Optional.ofNullable(paidAt); }
    public Optional<Instant> cancelledAt() { return Optional.ofNullable(cancelledAt); }

    // -------------------------------------------------------------------------
    // Domain Events
    // -------------------------------------------------------------------------

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> deduped = domainEvents.stream()
                .collect(Collectors.toMap(
                    DomainEvent::eventId, e -> e, (a, b) -> a, LinkedHashMap::new
                ))
                .values().stream()
                .sorted(Comparator.comparing(DomainEvent::occurredAt))
                .toList();
        domainEvents.clear();
        return deduped;
    }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Invoice i)) return false;
        return id.equals(i.id);
    }

    @Override public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return "Invoice{id=" + id + ", status=" + status.displayName()
             + ", total=" + total() + ", items=" + items.size() + "}";
    }

    // -------------------------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------------------------

    private void requireStatus(InvoiceStatus expected, String action) {
        if (this.status != expected) {
            throw new IllegalTransitionException(
                "Impossible de " + action + " une facture en état "
                + status.displayName() + (status.isTerminal() ? " [état terminal]" : "")
                + " (attendu : " + expected.displayName() + ")"
            );
        }
    }

    private Currency defaultCurrency() {
        return items.isEmpty() ? Currency.XOF : items.getFirst().unitPrice().currency();
    }

    // -------------------------------------------------------------------------
    // Exceptions
    // -------------------------------------------------------------------------

    public static final class SelfInvoicingException extends RuntimeException {
        public SelfInvoicingException(String message) { super(message); }
    }

    public static final class InvoiceAlreadySentException extends RuntimeException {
        public InvoiceAlreadySentException(String message) { super(message); }
    }

    public static final class InvoiceCannotBeSentException extends RuntimeException {
        public InvoiceCannotBeSentException(String message) { super(message); }
    }

    public static final class CurrencyMismatchException extends RuntimeException {
        public CurrencyMismatchException(String message) { super(message); }
    }

    public static final class IllegalTransitionException extends RuntimeException {
        public IllegalTransitionException(String message) { super(message); }
    }

    // -------------------------------------------------------------------------
    // Domain Events
    // -------------------------------------------------------------------------

    public record InvoiceSentEvent(
            UUID      eventId,
            InvoiceId invoiceId,
            UserId    issuerId,
            UserId    clientId,
            Money     total,
            Instant   occurredAt
    ) implements DomainEvent {

        public InvoiceSentEvent(InvoiceId invoiceId, UserId issuerId, UserId clientId, Money total, Instant occurredAt) {
            this(UUID.randomUUID(), invoiceId, issuerId, clientId, total, occurredAt);
        }
    }

    public record InvoicePaidEvent(
            UUID      eventId,
            InvoiceId invoiceId,
            UserId    issuerId,
            UserId    clientId,
            Money     total,
            Instant   occurredAt
    ) implements DomainEvent {

        public InvoicePaidEvent(InvoiceId invoiceId, UserId issuerId, UserId clientId, Money total, Instant occurredAt) {
            this(UUID.randomUUID(), invoiceId, issuerId, clientId, total, occurredAt);
        }
    }
}
