package com.uzima.domain.payment.model;

import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate Root : Portefeuille interne Uzima (WALLET).
 * <p>
 * Représente le solde applicatif d'un utilisateur.
 * Cycle de vie :
 * - Créé lors de l'inscription de l'utilisateur avec un solde initial à zéro.
 * - Débité via {@link #debit(Money, TimeProvider)} lors d'un paiement sortant.
 * - Crédité via {@link #credit(Money, TimeProvider)} lors d'un paiement entrant.
 * <p>
 * Invariants :
 * - Le solde ne peut jamais être négatif (géré par {@link Money#subtract(Money)}).
 * - Toutes les opérations exigent la même devise que le solde courant.
 * <p>
 * Domain Events émis :
 * - {@link WalletDebitedEvent} après chaque débit
 * - {@link WalletCreditedEvent} après chaque crédit
 */
public final class Wallet {

    private final WalletId id;
    private final UserId ownerId;
    private Money balance;
    private Instant updatedAt;

    private final Instant createdAt;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructeur privé
    // -------------------------------------------------------------------------

    private Wallet(WalletId id, UserId ownerId, Money balance, Instant createdAt, Instant updatedAt) {
        this.id        = Objects.requireNonNull(id,        "L'identifiant du portefeuille est obligatoire");
        this.ownerId   = Objects.requireNonNull(ownerId,   "Le propriétaire est obligatoire");
        this.balance   = Objects.requireNonNull(balance,   "Le solde est obligatoire");
        this.createdAt = Objects.requireNonNull(createdAt, "La date de création est obligatoire");
        this.updatedAt = Objects.requireNonNull(updatedAt, "La date de mise à jour est obligatoire");
    }

    // -------------------------------------------------------------------------
    // Factory : create() — nouveau portefeuille
    // -------------------------------------------------------------------------

    /**
     * Crée un portefeuille vide pour un nouvel utilisateur.
     * Le solde initial est zéro dans la devise spécifiée.
     */
    public static Wallet create(UserId ownerId, Currency currency, TimeProvider clock) {
        Objects.requireNonNull(ownerId,   "Le propriétaire est obligatoire");
        Objects.requireNonNull(currency,  "La devise est obligatoire");
        Objects.requireNonNull(clock,     "Le fournisseur de temps est obligatoire");

        Instant now = clock.now();
        return new Wallet(WalletId.generate(), ownerId, Money.zero(currency), now, now);
    }

    // -------------------------------------------------------------------------
    // Factory : reconstitute() — hydratation depuis la base de données
    // -------------------------------------------------------------------------

    public static Wallet reconstitute(
            WalletId id,
            UserId ownerId,
            Money balance,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Wallet(id, ownerId, balance, createdAt, updatedAt);
    }

    // -------------------------------------------------------------------------
    // Comportements métier
    // -------------------------------------------------------------------------

    /**
     * Débite le portefeuille du montant spécifié.
     *
     * @throws Money.InsufficientFundsException  si le solde est insuffisant
     * @throws Money.CurrencyMismatchException   si les devises diffèrent
     */
    public void debit(Money amount, TimeProvider clock) {
        Objects.requireNonNull(amount, "Le montant à débiter est obligatoire");
        Objects.requireNonNull(clock,  "Le fournisseur de temps est obligatoire");

        Money newBalance = this.balance.subtract(amount); // lève InsufficientFundsException si < 0
        this.balance   = newBalance;
        this.updatedAt = clock.now();

        domainEvents.add(new WalletDebitedEvent(this.id, this.ownerId, amount, this.balance, this.updatedAt));
    }

    /**
     * Crédite le portefeuille du montant spécifié.
     *
     * @throws Money.CurrencyMismatchException si les devises diffèrent
     */
    public void credit(Money amount, TimeProvider clock) {
        Objects.requireNonNull(amount, "Le montant à créditer est obligatoire");
        Objects.requireNonNull(clock,  "Le fournisseur de temps est obligatoire");

        this.balance   = this.balance.add(amount);
        this.updatedAt = clock.now();

        domainEvents.add(new WalletCreditedEvent(this.id, this.ownerId, amount, this.balance, this.updatedAt));
    }

    // -------------------------------------------------------------------------
    // Domain Events
    // -------------------------------------------------------------------------

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    public WalletId id()        { return id; }
    public UserId ownerId()     { return ownerId; }
    public Money balance()      { return balance; }
    public Instant createdAt()  { return createdAt; }
    public Instant updatedAt()  { return updatedAt; }

    // -------------------------------------------------------------------------
    // equals / hashCode (identité par ID)
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wallet w)) return false;
        return id.equals(w.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return "Wallet{id=" + id + ", owner=" + ownerId + ", balance=" + balance + "}";
    }

    // -------------------------------------------------------------------------
    // Domain Events (records imbriqués)
    // -------------------------------------------------------------------------

    public record WalletDebitedEvent(
            UUID eventId,
            WalletId walletId,
            UserId ownerId,
            Money amount,
            Money balanceAfter,
            Instant occurredAt
    ) implements DomainEvent {

        public WalletDebitedEvent(WalletId walletId, UserId ownerId, Money amount,
                                  Money balanceAfter, Instant occurredAt) {
            this(UUID.randomUUID(), walletId, ownerId, amount, balanceAfter, occurredAt);
        }
    }

    public record WalletCreditedEvent(
            UUID eventId,
            WalletId walletId,
            UserId ownerId,
            Money amount,
            Money balanceAfter,
            Instant occurredAt
    ) implements DomainEvent {

        public WalletCreditedEvent(WalletId walletId, UserId ownerId, Money amount,
                                   Money balanceAfter, Instant occurredAt) {
            this(UUID.randomUUID(), walletId, ownerId, amount, balanceAfter, occurredAt);
        }
    }
}
