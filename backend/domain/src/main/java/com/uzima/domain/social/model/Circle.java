package com.uzima.domain.social.model;

import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.time.Instant;
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
 * Aggregate Root : Cercle de Vie.Un cercle regroupe des utilisateurs selon un type de relation (famille, travail…).
 * Il est gouverné par des règles (CircleRule) et une liste de memberships.
 * Invariants protégés :
 * - Un utilisateur ne peut appartenir qu'une seule fois au même cercle
 * - Le propriétaire (OWNER) ne peut pas être retiré
 * - Seul un ADMIN ou OWNER peut modifier les règles et le nom
 * - Le nom est obligatoire et limité à 100 caractères
 * Domain Events émis :
 * - CircleCreatedEvent (à la création)
 * - MemberAddedEvent     (à chaque ajout)
 * - MemberRemovedEvent   (à chaque retrait)
 */
public final class Circle {

    // -------------------------------------------------------------------------
    // Champs
    // -------------------------------------------------------------------------

    private final CircleId  id;
    private       String    name;
    private final CircleType type;
    private final UserId    ownerId;
    private       CircleRule rules;
    private final Instant   createdAt;

    private final List<CircleMembership> memberships  = new ArrayList<>();
    private final List<DomainEvent>      domainEvents = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructeur privé
    // -------------------------------------------------------------------------

    private Circle(
            CircleId  id,
            String    name,
            CircleType type,
            UserId    ownerId,
            CircleRule rules,
            Instant   createdAt
    ) {
        this.id        = Objects.requireNonNull(id,        "L'identifiant est obligatoire");
        this.type      = Objects.requireNonNull(type,      "Le type est obligatoire");
        this.ownerId   = Objects.requireNonNull(ownerId,   "Le propriétaire est obligatoire");
        this.rules     = Objects.requireNonNull(rules,     "Les règles sont obligatoires");
        this.createdAt = Objects.requireNonNull(createdAt, "La date de création est obligatoire");
        this.name      = validateName(name);
    }

    // -------------------------------------------------------------------------
    // Factory : create() — nouvelle création
    // -------------------------------------------------------------------------

    /**
     * Crée un nouveau cercle avec le propriétaire automatiquement ajouté en tant qu'OWNER.
     *
     * @throws InvalidCircleNameException si name null ou > 100 caractères
     */
    public static Circle create(String name, CircleType type, UserId ownerId, TimeProvider clock) {
        Objects.requireNonNull(type,    "Le type est obligatoire");
        Objects.requireNonNull(ownerId, "Le propriétaire est obligatoire");
        Objects.requireNonNull(clock,   "Le fournisseur de temps est obligatoire");

        CircleId   id        = CircleId.generate();
        CircleRule defaults  = CircleRule.defaultForType(type);
        Instant    now       = clock.now();

        Circle circle = new Circle(id, name, type, ownerId, defaults, now);
        circle.memberships.add(new CircleMembership(ownerId, MemberRole.OWNER, now));
        circle.domainEvents.add(new CircleCreatedEvent(id, ownerId, type, now));
        return circle;
    }

    // -------------------------------------------------------------------------
    // Factory : reconstitute() — hydratation depuis la base de données
    // -------------------------------------------------------------------------

    /**
     * Reconstruit un cercle depuis la persistance. N'émet PAS d'événements.
     */
    public static Circle reconstitute(
            CircleId             id,
            String               name,
            CircleType           type,
            UserId               ownerId,
            CircleRule           rules,
            Instant              createdAt,
            List<CircleMembership> memberships
    ) {
        Circle circle = new Circle(id, name, type, ownerId, rules, createdAt);
        circle.memberships.addAll(memberships);
        return circle;
    }

    // -------------------------------------------------------------------------
    // Comportements métier
    // -------------------------------------------------------------------------

    /**
     * Ajoute un membre au cercle.
     *
     * @throws DuplicateMemberException si l'utilisateur est déjà membre
     */
    public void addMember(UserId userId, MemberRole role, TimeProvider clock) {
        Objects.requireNonNull(userId, "L'identifiant du membre est obligatoire");
        Objects.requireNonNull(role,   "Le rôle est obligatoire");
        Objects.requireNonNull(clock,  "Le fournisseur de temps est obligatoire");

        if (role == MemberRole.OWNER) {
            throw new IllegalArgumentException("Un cercle ne peut avoir qu'un seul OWNER");
        }
        if (isMember(userId)) {
            throw new DuplicateMemberException(
                "L'utilisateur " + userId + " est déjà membre du cercle " + id
            );
        }

        Instant now = clock.now();
        memberships.add(new CircleMembership(userId, role, now));
        domainEvents.add(new MemberAddedEvent(id, userId, role, now));
    }

    /**
     * Retire un membre du cercle.
     *
     * @throws OwnerCannotLeaveException si userId est le propriétaire
     * @throws MemberNotFoundException   si userId n'est pas membre
     */
    public void removeMember(UserId userId, TimeProvider clock) {
        Objects.requireNonNull(userId, "L'identifiant du membre est obligatoire");
        Objects.requireNonNull(clock,  "Le fournisseur de temps est obligatoire");

        if (ownerId.equals(userId)) {
            throw new OwnerCannotLeaveException(
                "Le propriétaire ne peut pas quitter son propre cercle (id=" + id + ")"
            );
        }
        CircleMembership membership = findMembership(userId)
                .orElseThrow(() -> new MemberNotFoundException(userId, id));

        memberships.remove(membership);
        domainEvents.add(new MemberRemovedEvent(id, userId, clock.now()));
    }

    /**
     * Met à jour les règles du cercle. Réservé aux ADMIN et OWNER.
     *
     * @throws InsufficientPermissionException si requesterId n'est pas ADMIN+
     */
    public void updateRules(CircleRule newRules, UserId requesterId) {
        Objects.requireNonNull(newRules,     "Les nouvelles règles sont obligatoires");
        Objects.requireNonNull(requesterId,  "L'identifiant du demandeur est obligatoire");

        requireAdminPermission(requesterId, "modifier les règles");
        this.rules = newRules;
    }

    /**
     * Renomme le cercle. Réservé aux ADMIN et OWNER.
     *
     * @throws InsufficientPermissionException si requesterId n'est pas ADMIN+
     * @throws InvalidCircleNameException      si le nom est invalide
     */
    public void rename(String newName, UserId requesterId) {
        Objects.requireNonNull(requesterId, "L'identifiant du demandeur est obligatoire");

        requireAdminPermission(requesterId, "renommer le cercle");
        this.name = validateName(newName);
    }

    // -------------------------------------------------------------------------
    // Prédicats
    // -------------------------------------------------------------------------

    public boolean isMember(UserId userId) {
        return memberships.stream().anyMatch(m -> m.isMember(userId));
    }

    public boolean isAdmin(UserId userId) {
        return findMembership(userId).map(CircleMembership::isAdmin).orElse(false);
    }

    public boolean isOwner(UserId userId) {
        return ownerId.equals(userId);
    }

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
    // Accesseurs
    // -------------------------------------------------------------------------

    public CircleId             id()          { return id; }
    public String               name()        { return name; }
    public CircleType           type()        { return type; }
    public UserId               ownerId()     { return ownerId; }
    public CircleRule           rules()       { return rules; }
    public Instant              createdAt()   { return createdAt; }
    public int                  memberCount() { return memberships.size(); }

    public List<CircleMembership> memberships() {
        return Collections.unmodifiableList(memberships);
    }

    public Optional<CircleMembership> membershipOf(UserId userId) {
        return findMembership(userId);
    }

    // -------------------------------------------------------------------------
    // equals / hashCode (identité par ID)
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Circle c)) return false;
        return id.equals(c.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Circle{id=" + id + ", name='" + name + "', type=" + type.displayName()
             + ", members=" + memberships.size() + "}";
    }

    // -------------------------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------------------------

    private Optional<CircleMembership> findMembership(UserId userId) {
        return memberships.stream().filter(m -> m.isMember(userId)).findFirst();
    }

    private void requireAdminPermission(UserId requesterId, String action) {
        CircleMembership requester = findMembership(requesterId)
                .orElseThrow(() -> new MemberNotFoundException(requesterId, id));
        if (!requester.isAdmin()) {
            throw new InsufficientPermissionException(
                "Permission insuffisante pour " + action
                + " : rôle actuel = " + requester.role().displayName()
                + " (ADMIN ou OWNER requis)"
            );
        }
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidCircleNameException("Le nom du cercle ne peut pas être vide");
        }
        if (name.length() > 100) {
            throw new InvalidCircleNameException(
                "Le nom du cercle ne peut pas dépasser 100 caractères"
            );
        }
        return name.strip();
    }

    // -------------------------------------------------------------------------
    // Exceptions domaine imbriquées
    // -------------------------------------------------------------------------

    public static final class InvalidCircleNameException extends RuntimeException {
        public InvalidCircleNameException(String message) { super(message); }
    }

    public static final class DuplicateMemberException extends RuntimeException {
        public DuplicateMemberException(String message) { super(message); }
    }

    public static final class OwnerCannotLeaveException extends RuntimeException {
        public OwnerCannotLeaveException(String message) { super(message); }
    }

    public static final class MemberNotFoundException extends RuntimeException {
        public MemberNotFoundException(UserId userId, CircleId circleId) {
            super("Membre introuvable : " + userId + " dans le cercle " + circleId);
        }
    }

    public static final class InsufficientPermissionException extends RuntimeException {
        public InsufficientPermissionException(String message) { super(message); }
    }

    // -------------------------------------------------------------------------
    // Domain Events (records imbriqués)
    // -------------------------------------------------------------------------

    public record CircleCreatedEvent(
            UUID      eventId,
            CircleId  circleId,
            UserId    ownerId,
            CircleType type,
            Instant   occurredAt
    ) implements DomainEvent {

        public CircleCreatedEvent(CircleId circleId, UserId ownerId, CircleType type, Instant occurredAt) {
            this(UUID.randomUUID(), circleId, ownerId, type, occurredAt);
        }
    }

    public record MemberAddedEvent(
            UUID       eventId,
            CircleId   circleId,
            UserId     memberId,
            MemberRole role,
            Instant    occurredAt
    ) implements DomainEvent {

        public MemberAddedEvent(CircleId circleId, UserId memberId, MemberRole role, Instant occurredAt) {
            this(UUID.randomUUID(), circleId, memberId, role, occurredAt);
        }
    }

    public record MemberRemovedEvent(
            UUID     eventId,
            CircleId circleId,
            UserId   memberId,
            Instant  occurredAt
    ) implements DomainEvent {

        public MemberRemovedEvent(CircleId circleId, UserId memberId, Instant occurredAt) {
            this(UUID.randomUUID(), circleId, memberId, occurredAt);
        }
    }
}
