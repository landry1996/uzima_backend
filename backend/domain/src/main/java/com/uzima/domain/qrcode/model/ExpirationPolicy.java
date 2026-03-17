package com.uzima.domain.qrcode.model;

import com.uzima.domain.shared.DomainException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Value Object : Politique d'expiration d'un QR Code.
 * Deux modes :
 * - PERMANENT : jamais expiré (sauf révocation manuelle)
 * - TIMED     : expiré après une date précise
 * Invariant : si une date d'expiration est fournie, elle doit être dans le futur
 * au moment de la création.
 */
public final class ExpirationPolicy {

    private final Instant expiresAt; // null = permanent

    private ExpirationPolicy(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * Crée une politique permanente (pas d'expiration automatique).
     */
    public static ExpirationPolicy permanent() {
        return new ExpirationPolicy(null);
    }

    /**
     * Crée une politique avec expiration à une date précise.
     *
     * @param expiresAt  La date d'expiration
     * @param now        L'instant actuel (fourni par TimeProvider, jamais Instant.now())
     */
    public static ExpirationPolicy expiresAt(Instant expiresAt, Instant now) {
        Objects.requireNonNull(expiresAt, "La date d'expiration ne peut pas être nulle");
        Objects.requireNonNull(now, "L'instant courant ne peut pas être nul");
        if (!expiresAt.isAfter(now)) {
            throw new ExpirationInThePastException(
                "La date d'expiration doit être dans le futur (reçu : " + expiresAt + ", maintenant : " + now + ")"
            );
        }
        return new ExpirationPolicy(expiresAt);
    }

    /**
     * Crée une politique avec une durée à partir de maintenant.
     *
     * @param duration   Durée avant expiration (ex: Duration.ofHours(24))
     * @param now        L'instant actuel (fourni par TimeProvider)
     */
    public static ExpirationPolicy expiresAfter(Duration duration, Instant now) {
        Objects.requireNonNull(duration, "La durée ne peut pas être nulle");
        Objects.requireNonNull(now, "L'instant courant ne peut pas être nul");
        if (duration.isNegative() || duration.isZero()) {
            throw new InvalidExpirationDurationException("La durée d'expiration doit être positive");
        }
        return expiresAt(now.plus(duration), now);
    }

    /**
     * Reconstitue une politique depuis la persistance, sans valider la date.
     * Utilisé uniquement par les mappers infrastructure pour recharger un QR code
     * dont la date d'expiration peut légitimement être dans le passé.
     *
     * @param expiresAt date d'expiration stockée en DB (non nulle)
     */
    public static ExpirationPolicy reconstitute(Instant expiresAt) {
        Objects.requireNonNull(expiresAt, "La date d'expiration ne peut pas être nulle");
        return new ExpirationPolicy(expiresAt);
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public boolean isExpiredAt(Instant checkTime) {
        Objects.requireNonNull(checkTime);
        return expiresAt != null && !checkTime.isBefore(expiresAt);
    }

    /**
     * Vérifie si ce QR Code expire aujourd'hui (date locale UTC).
     * Utilisé par le service de notifications pour alerter le propriétaire
     * que son QR Code (TEMPORARY_LOCATION, EVENT) va expirer dans la journée.
     * Exemple d'usage de TimeProvider.today() dans le domaine.
     *
     * @param today La date du jour (fournie par TimeProvider.today(), jamais LocalDate.now())
     */
    public boolean expiresOnDate(LocalDate today) {
        Objects.requireNonNull(today, "La date du jour est obligatoire");
        if (isPermanent()) return false;
        LocalDate expiryDate = expiresAt.atZone(ZoneOffset.UTC).toLocalDate();
        return expiryDate.equals(today);
    }

    public java.util.Optional<Instant> expiresAt() {
        return java.util.Optional.ofNullable(expiresAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExpirationPolicy e)) return false;
        return Objects.equals(expiresAt, e.expiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(expiresAt);
    }

    public static final class ExpirationInThePastException extends DomainException {
        public ExpirationInThePastException(String m) { super(m); }
    }

    public static final class InvalidExpirationDurationException extends DomainException {
        public InvalidExpirationDurationException(String m) { super(m); }
    }
}
