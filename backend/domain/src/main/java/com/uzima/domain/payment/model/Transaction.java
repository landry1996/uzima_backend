package com.uzima.domain.payment.model;

import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aggregate Root : Transaction de paiement.
 * Cycle de vie :
 *   PENDING → COMPLETED (via complete())
 *   PENDING → FAILED (via fail())
 *   PENDING → CANCELLED (via cancel())
 * Invariants protégés :
 * - L'expéditeur ne peut pas être le destinataire
 * - Le montant doit être strictement positif
 * - Les transitions d'état sont contrôlées (pas de modification d'un état terminal)
 * - La description est optionnelle mais limitée à 255 caractères si fournie
 * Domain Events émis :
 * - TransactionInitiatedEvent (à l'initiation)
 * - TransactionCompletedEvent  (après succès gateway)
 * - TransactionFailedEvent     (après échec gateway)
 */
public final class Transaction {

    // -------------------------------------------------------------------------
    // Champs (tous final = immutabilité structurelle)
    // -------------------------------------------------------------------------

    private final TransactionId id;
    private final UserId senderId;
    private final UserId recipientId;
    private final Money amount;
    private final PaymentMethod method;
    private final String description;
    private final Instant initiatedAt;

    // Champs mutables (cycle de vie contrôlé par les méthodes de comportement)
    private TransactionStatus status;
    private String externalId;      // ID retourné par la gateway externe
    private String failureReason;
    private Instant completedAt;
    private Instant failedAt;
    private Instant cancelledAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructeur privé
    // -------------------------------------------------------------------------

    private Transaction(
            TransactionId id,
            UserId senderId,
            UserId recipientId,
            Money amount,
            PaymentMethod method,
            String description,
            TransactionStatus status,
            String externalId,
            String failureReason,
            Instant initiatedAt,
            Instant completedAt,
            Instant failedAt,
            Instant cancelledAt
    ) {
        this.id            = Objects.requireNonNull(id, "L'identifiant est obligatoire");
        this.senderId      = Objects.requireNonNull(senderId, "L'expéditeur est obligatoire");
        this.recipientId   = Objects.requireNonNull(recipientId, "Le destinataire est obligatoire");
        this.amount        = Objects.requireNonNull(amount, "Le montant est obligatoire");
        this.method        = Objects.requireNonNull(method, "La méthode de paiement est obligatoire");
        this.status        = Objects.requireNonNull(status, "Le statut est obligatoire");
        this.initiatedAt   = Objects.requireNonNull(initiatedAt, "La date d'initiation est obligatoire");
        this.description   = description;
        this.externalId    = externalId;
        this.failureReason = failureReason;
        this.completedAt   = completedAt;
        this.failedAt      = failedAt;
        this.cancelledAt   = cancelledAt;
    }

    // -------------------------------------------------------------------------
    // Factory : initiate() — création d'une nouvelle transaction
    // -------------------------------------------------------------------------

    /**
     * Crée une nouvelle transaction en état PENDING.
     *
     * @throws SelfPaymentException       si sender == recipient
     * @throws NonPositiveAmountException si amount <= 0
     */
    public static Transaction initiate(
            UserId senderId,
            UserId recipientId,
            Money amount,
            PaymentMethod method,
            String description,
            TimeProvider clock
    ) {
        Objects.requireNonNull(senderId, "L'expéditeur est obligatoire");
        Objects.requireNonNull(recipientId, "Le destinataire est obligatoire");
        Objects.requireNonNull(amount, "Le montant est obligatoire");
        Objects.requireNonNull(method, "La méthode de paiement est obligatoire");
        Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");

        if (senderId.equals(recipientId)) {
            throw new SelfPaymentException(
                "L'expéditeur et le destinataire ne peuvent pas être identiques : " + senderId
            );
        }
        if (!amount.isPositive()) {
            throw new NonPositiveAmountException(
                "Le montant d'une transaction doit être strictement positif : " + amount
            );
        }
        if (description != null && description.length() > 255) {
            throw new InvalidDescriptionException(
                "La description ne peut pas dépasser 255 caractères"
            );
        }

        Transaction tx = new Transaction(
            TransactionId.generate(),
            senderId,
            recipientId,
            amount,
            method,
            description,
            TransactionStatus.PENDING,
            null, null,
            clock.now(),
            null, null, null
        );

        tx.domainEvents.add(new TransactionInitiatedEvent(tx.id, senderId, recipientId, amount, clock.now()));
        return tx;
    }

    // -------------------------------------------------------------------------
    // Factory : reconstitute() — hydratation depuis la base de données
    // -------------------------------------------------------------------------

    /**
     * Reconstruit une transaction depuis la persistance.
     * N'émet PAS d'événements de domaine.
     */
    public static Transaction reconstitute(
            TransactionId id,
            UserId senderId,
            UserId recipientId,
            Money amount,
            PaymentMethod method,
            String description,
            TransactionStatus status,
            String externalId,
            String failureReason,
            Instant initiatedAt,
            Instant completedAt,
            Instant failedAt,
            Instant cancelledAt
    ) {
        return new Transaction(
            id, senderId, recipientId, amount, method, description,
            status, externalId, failureReason,
            initiatedAt, completedAt, failedAt, cancelledAt
        );
    }

    // -------------------------------------------------------------------------
    // Comportements métier
    // -------------------------------------------------------------------------

    /**
     * Marque la transaction comme réussie après confirmation de la gateway.
     *
     * @param externalId Identifiant retourné par la gateway externe
     * @param clock      Fournisseur de temps
     * @throws IllegalTransitionException si la transaction n'est pas PENDING
     */
    public void complete(String externalId, TimeProvider clock) {
        Objects.requireNonNull(externalId, "L'identifiant externe est obligatoire");
        Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
        requireStatus("compléter");

        this.status      = TransactionStatus.COMPLETED;
        this.externalId  = externalId;
        this.completedAt = clock.now();

        domainEvents.add(new TransactionCompletedEvent(this.id, externalId, this.completedAt));
    }

    /**
     * Marque la transaction comme échouée.
     *
     * @param reason Raison de l'échec (message d'erreur gateway)
     * @param clock  Fournisseur de temps
     * @throws IllegalTransitionException si la transaction n'est pas PENDING
     */
    public void fail(String reason, TimeProvider clock) {
        Objects.requireNonNull(reason, "La raison d'échec est obligatoire");
        Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
        requireStatus("échouer");

        this.status        = TransactionStatus.FAILED;
        this.failureReason = reason;
        this.failedAt      = clock.now();

        domainEvents.add(new TransactionFailedEvent(this.id, reason, this.failedAt));
    }

    /**
     * Annule la transaction si elle est encore PENDING.
     *
     * @throws IllegalTransitionException si la transaction n'est pas PENDING
     */
    public void cancel(TimeProvider clock) {
        Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
        if (!canBeCancelled()) {
            throw new IllegalTransitionException(
                "Impossible d'annuler une transaction en état " + this.status.displayName()
            );
        }
        this.status      = TransactionStatus.CANCELLED;
        this.cancelledAt = clock.now();
    }

    // -------------------------------------------------------------------------
    // Prédicats
    // -------------------------------------------------------------------------

    public boolean canBeCancelled() {
        return isPending();
    }

    public boolean isCompleted() {
        return this.status == TransactionStatus.COMPLETED;
    }

    public boolean isFailed() {
        return this.status == TransactionStatus.FAILED;
    }

    public boolean isPending() {
        return this.status == TransactionStatus.PENDING;
    }

    // -------------------------------------------------------------------------
    // Domain Events
    // -------------------------------------------------------------------------

    /**
     * Draine et retourne tous les événements de domaine accumulés.
     * Déduplication par eventId, tri chronologique.
     * Vide la liste après appel.
     */
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
    // Accesseurs
    // -------------------------------------------------------------------------

    public TransactionId id()           { return id; }
    public UserId senderId()            { return senderId; }
    public UserId recipientId()         { return recipientId; }
    public Money amount()               { return amount; }
    public PaymentMethod method()       { return method; }
    public TransactionStatus status()   { return status; }
    public Instant initiatedAt()        { return initiatedAt; }

    public Optional<String> description()   { return Optional.ofNullable(description); }
    public Optional<String> externalId()    { return Optional.ofNullable(externalId); }
    public Optional<String> failureReason() { return Optional.ofNullable(failureReason); }
    public Optional<Instant> completedAt()  { return Optional.ofNullable(completedAt); }
    public Optional<Instant> failedAt()     { return Optional.ofNullable(failedAt); }
    public Optional<Instant> cancelledAt()  { return Optional.ofNullable(cancelledAt); }

    // -------------------------------------------------------------------------
    // equals / hashCode (identité par ID)
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction t)) return false;
        return id.equals(t.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Transaction{id=" + id + ", status=" + status.displayName()
             + ", amount=" + amount + ", method=" + method.displayName() + "}";
    }

    // -------------------------------------------------------------------------
    // Helper privé
    // -------------------------------------------------------------------------

    private void requireStatus(String action) {
        if (this.status != TransactionStatus.PENDING) {
            String terminalHint = this.status.isTerminal() ? " [état terminal]" : "";
            throw new IllegalTransitionException(
                "Impossible de " + action + " une transaction en état "
                + this.status.displayName() + terminalHint
                + " (attendu : " + TransactionStatus.PENDING.displayName() + ")"
            );
        }
    }

    // -------------------------------------------------------------------------
    // Exceptions domaine imbriquées
    // -------------------------------------------------------------------------

    public static final class SelfPaymentException extends RuntimeException {
        public SelfPaymentException(String message) { super(message); }
    }

    public static final class NonPositiveAmountException extends RuntimeException {
        public NonPositiveAmountException(String message) { super(message); }
    }

    public static final class InvalidDescriptionException extends RuntimeException {
        public InvalidDescriptionException(String message) { super(message); }
    }

    public static final class IllegalTransitionException extends RuntimeException {
        public IllegalTransitionException(String message) { super(message); }
    }

    // -------------------------------------------------------------------------
    // Domain Events (records imbriqués)
    // -------------------------------------------------------------------------

    public record TransactionInitiatedEvent(
            UUID eventId,
            TransactionId transactionId,
            UserId senderId,
            UserId recipientId,
            Money amount,
            Instant occurredAt
    ) implements DomainEvent {

        public TransactionInitiatedEvent(
                TransactionId transactionId,
                UserId senderId,
                UserId recipientId,
                Money amount,
                Instant occurredAt
        ) {
            this(UUID.randomUUID(), transactionId, senderId, recipientId, amount, occurredAt);
        }
    }

    public record TransactionCompletedEvent(
            UUID eventId,
            TransactionId transactionId,
            String externalId,
            Instant occurredAt
    ) implements DomainEvent {

        public TransactionCompletedEvent(TransactionId transactionId, String externalId, Instant occurredAt) {
            this(UUID.randomUUID(), transactionId, externalId, occurredAt);
        }
    }

    public record TransactionFailedEvent(
            UUID eventId,
            TransactionId transactionId,
            String reason,
            Instant occurredAt
    ) implements DomainEvent {

        public TransactionFailedEvent(TransactionId transactionId, String reason, Instant occurredAt) {
            this(UUID.randomUUID(), transactionId, reason, occurredAt);
        }
    }
}
