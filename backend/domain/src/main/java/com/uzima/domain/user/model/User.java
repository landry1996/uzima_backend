package com.uzima.domain.user.model;

import com.uzima.domain.shared.DomainException;
import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.TimeProvider;

import java.time.Instant;
import java.util.*;

/**
 * Aggregate Root : Utilisateur.
 * Responsabilités métier :
 * - Inscription et validation des données d'entrée
 * - Gestion de l'état de présence (Nouveauté 6, 8)
 * - Gestion du statut premium
 * Invariants garantis par construction :
 * - Un utilisateur a toujours un ID, un phoneNumber, firstName, lastName, country valides
 * - La date de création est toujours définie et ne peut pas être modifiée
 * - Le passwordHash ne peut être ni nul, ni vide
 * INTERDIT : @Builder, @Setter, @Data, Lombok, Spring, JPA
 * Le constructeur public est privé pour forcer l'usage des factory methods.
 */
public final class User {

    private final UserId id;
    private final PhoneNumber phoneNumber;
    private final CountryCode country;
    private final Instant createdAt;

    // Mutable (état géré par le domaine)
    private FirstName firstName;
    private LastName lastName;
    private String avatarUrl;
    private PresenceStatus presenceStatus;
    private boolean premium;
    private final String passwordHash;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // Constructeur privé : création uniquement via factory methods
    private User(
            UserId id,
            PhoneNumber phoneNumber,
            CountryCode country,
            FirstName firstName,
            LastName lastName,
            String passwordHash,
            Instant createdAt,
            PresenceStatus presenceStatus,
            boolean premium,
            String avatarUrl
    ) {
        this.id = Objects.requireNonNull(id);
        this.phoneNumber = Objects.requireNonNull(phoneNumber);
        this.country = Objects.requireNonNull(country);
        this.firstName = Objects.requireNonNull(firstName);
        this.lastName = Objects.requireNonNull(lastName);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.presenceStatus = Objects.requireNonNull(presenceStatus);
        this.premium = premium;
        this.avatarUrl = avatarUrl;
    }

    /**
     * Factory method : Crée un nouvel utilisateur.
     * <p>
     * Tous les invariants sont vérifiés avant la création.
     * Un événement UserRegistered est enregistré.
     *
     * @param phoneNumber  Le numéro de téléphone (sera validé par le VO)
     * @param country      Le code pays ISO 3166-1 alpha-2 (ex: "CM", "FR")
     * @param firstName    Le prénom
     * @param lastName     Le nom de famille
     * @param passwordHash Le hash du mot de passe (fourni par l'infrastructure)
     * @param clock        Le fournisseur de temps injecté (jamais Instant.now() direct)
     */
    public static User register(
            PhoneNumber phoneNumber,
            CountryCode country,
            FirstName firstName,
            LastName lastName,
            String passwordHash,
            TimeProvider clock
    ) {
        Objects.requireNonNull(clock, "Le TimeProvider est obligatoire");
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new InvalidPasswordHashException("Le hash du mot de passe ne peut pas être vide");
        }

        UserId id = UserId.generate();
        Instant now = clock.now();

        User user = new User(
                id,
                phoneNumber,
                country,
                firstName,
                lastName,
                passwordHash,
                now,
                PresenceStatus.AVAILABLE,
                false,
                null
        );

        user.domainEvents.add(new UserRegisteredEvent(id, phoneNumber, country, now));
        return user;
    }

    /**
     * Factory method : Reconstitue un utilisateur depuis la persistance.
     * N'émet PAS d'événement de domaine (reconstitution, pas création).
     */
    public static User reconstitute(
            UserId id,
            PhoneNumber phoneNumber,
            CountryCode country,
            FirstName firstName,
            LastName lastName,
            String passwordHash,
            Instant createdAt,
            PresenceStatus presenceStatus,
            boolean premium,
            String avatarUrl
    ) {
        return new User(id, phoneNumber, country, firstName, lastName, passwordHash, createdAt, presenceStatus, premium, avatarUrl);
    }

    // -------------------------------------------------------------------------
    // Comportements métier
    // -------------------------------------------------------------------------

    /**
     * Met à jour l'état de présence de l'utilisateur (Nouveauté 6).
     */
    public void updatePresenceStatus(PresenceStatus newStatus, TimeProvider clock) {
        Objects.requireNonNull(newStatus, "Le nouvel état de présence ne peut pas être nul");
        Objects.requireNonNull(clock, "Le TimeProvider est obligatoire");

        PresenceStatus previous = this.presenceStatus;
        this.presenceStatus = newStatus;
        this.domainEvents.add(new PresenceStatusUpdatedEvent(this.id, previous, newStatus, clock.now()));
    }

    /**
     * Passe l'utilisateur en mode premium.
     * Idempotent : pas d'erreur si déjà premium.
     */
    public void upgradeToPremium(TimeProvider clock) {
        Objects.requireNonNull(clock, "Le TimeProvider est obligatoire");
        if (!this.premium) {
            this.premium = true;
            this.domainEvents.add(new UserUpgradedToPremiumEvent(this.id, clock.now()));
        }
    }

    /**
     * Met à jour le prénom et le nom de famille.
     */
    public void changeName(FirstName newFirstName, LastName newLastName) {
        Objects.requireNonNull(newFirstName, "Le nouveau prénom ne peut pas être nul");
        Objects.requireNonNull(newLastName, "Le nouveau nom ne peut pas être nul");
        this.firstName = newFirstName;
        this.lastName = newLastName;
    }

    /**
     * Met à jour l'URL de l'avatar.
     */
    public void updateAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /**
     * Retourne et vide les événements de domaine accumulés.
     * À appeler par le repository après la persistance.
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    // -------------------------------------------------------------------------
    // Accesseurs (pas de setters publics)
    // -------------------------------------------------------------------------

    public UserId id() { return id; }
    public PhoneNumber phoneNumber() { return phoneNumber; }
    public CountryCode country() { return country; }
    public FirstName firstName() { return firstName; }
    public LastName lastName() { return lastName; }
    public String fullName() { return firstName.value() + " " + lastName.value(); }
    public Optional<String> avatarUrl() { return Optional.ofNullable(avatarUrl); }
    public PresenceStatus presenceStatus() { return presenceStatus; }
    public boolean isPremium() { return premium; }
    public String passwordHash() { return passwordHash; }
    public Instant createdAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", phone=" + phoneNumber
                + ", country=" + country.displayName()
                + ", name=" + firstName + " " + lastName + "}";
    }

    // -------------------------------------------------------------------------
    // Événements de domaine
    // -------------------------------------------------------------------------

    public record UserRegisteredEvent(
            UUID eventId,
            UserId userId,
            PhoneNumber phoneNumber,
            CountryCode country,
            Instant occurredAt
    ) implements DomainEvent {
        public UserRegisteredEvent(UserId userId, PhoneNumber phoneNumber, CountryCode country, Instant occurredAt) {
            this(UUID.randomUUID(), userId, phoneNumber, country, occurredAt);
        }
    }

    public record PresenceStatusUpdatedEvent(
            UUID eventId,
            UserId userId,
            PresenceStatus previousStatus,
            PresenceStatus newStatus,
            Instant occurredAt
    ) implements DomainEvent {
        public PresenceStatusUpdatedEvent(
                UserId userId, PresenceStatus previousStatus,
                PresenceStatus newStatus, Instant occurredAt
        ) {
            this(UUID.randomUUID(), userId, previousStatus, newStatus, occurredAt);
        }
    }

    public record UserUpgradedToPremiumEvent(
            UUID eventId,
            UserId userId,
            Instant occurredAt
    ) implements DomainEvent {
        public UserUpgradedToPremiumEvent(UserId userId, Instant occurredAt) {
            this(UUID.randomUUID(), userId, occurredAt);
        }
    }

    // -------------------------------------------------------------------------
    // Exceptions de domaine
    // -------------------------------------------------------------------------

    public static final class InvalidPasswordHashException extends DomainException {
        public InvalidPasswordHashException(String message) {
            super(message);
        }
    }
}
