package com.uzima.domain.wellbeing.model;

import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Entity : Session d'utilisation d'une application.
 * <p>
 * Représente une période d'utilisation d'une app identifiée par son nom,
 * catégorisée par AppType.
 * <p>
 * Invariants :
 * - appName obligatoire, non vide
 * - endedAt >= startedAt si présent
 */
public final class UsageSession {

    private final UsageSessionId id;
    private final UserId         userId;
    private final String         appName;
    private final AppType        appType;
    private final Instant        startedAt;
    private       Instant        endedAt;

    // -------------------------------------------------------------------------
    // Constructeur privé
    // -------------------------------------------------------------------------

    private UsageSession(UsageSessionId id, UserId userId, String appName, AppType appType,
                         Instant startedAt, Instant endedAt) {
        this.id        = Objects.requireNonNull(id,      "L'identifiant est obligatoire");
        this.userId    = Objects.requireNonNull(userId,  "L'identifiant utilisateur est obligatoire");
        this.appName   = validateAppName(appName);
        this.appType   = Objects.requireNonNull(appType, "Le type d'application est obligatoire");
        this.startedAt = Objects.requireNonNull(startedAt, "La date de début est obligatoire");
        this.endedAt   = endedAt;
    }

    // -------------------------------------------------------------------------
    // Factory : track()
    // -------------------------------------------------------------------------

    /** Démarre le suivi d'une session d'utilisation. */
    public static UsageSession track(UserId userId, String appName, AppType appType,
                                     TimeProvider clock) {
        Objects.requireNonNull(userId,  "L'identifiant utilisateur est obligatoire");
        Objects.requireNonNull(appType, "Le type d'application est obligatoire");
        Objects.requireNonNull(clock,   "Le fournisseur de temps est obligatoire");
        return new UsageSession(UsageSessionId.generate(), userId, appName, appType,
                                clock.now(), null);
    }

    // -------------------------------------------------------------------------
    // Factory : reconstitute()
    // -------------------------------------------------------------------------

    public static UsageSession reconstitute(UsageSessionId id, UserId userId, String appName,
                                             AppType appType, Instant startedAt, Instant endedAt) {
        return new UsageSession(id, userId, appName, appType, startedAt, endedAt);
    }

    // -------------------------------------------------------------------------
    // Comportements métier
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Computed
    // -------------------------------------------------------------------------

    /** Durée effective. Vide si la session est encore ouverte. */
    public Optional<Duration> duration() {
        if (endedAt == null) return Optional.empty();
        return Optional.of(Duration.between(startedAt, endedAt));
    }

    public boolean isProductiveApp() { return appType.isProductive(); }

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    public UsageSessionId    id()        { return id; }
    public UserId            userId()    { return userId; }
    public String            appName()   { return appName; }
    public AppType           appType()   { return appType; }
    public Instant           startedAt() { return startedAt; }
    public Optional<Instant> endedAt()   { return Optional.ofNullable(endedAt); }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsageSession s)) return false;
        return id.equals(s.id);
    }

    @Override public int hashCode() { return id.hashCode(); }

    @Override
    public String toString() {
        return "UsageSession{id=" + id + ", app=" + appName + ", type=" + appType + "}";
    }

    // -------------------------------------------------------------------------
    // Helper privé
    // -------------------------------------------------------------------------

    private static String validateAppName(String appName) {
        if (appName == null || appName.isBlank()) {
            throw new InvalidAppNameException("Le nom de l'application ne peut pas être vide");
        }
        if (appName.length() > 100) {
            throw new InvalidAppNameException("Le nom de l'application ne peut pas dépasser 100 caractères");
        }
        return appName.strip();
    }

    // -------------------------------------------------------------------------
    // Exception
    // -------------------------------------------------------------------------

    public static final class InvalidAppNameException extends RuntimeException {
        public InvalidAppNameException(String message) { super(message); }
    }
}
