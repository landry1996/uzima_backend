package com.uzima.domain.qrcode.model;

import com.uzima.domain.shared.DomainException;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate Root : QR Code Contextuel Intelligent (Innovation Brevetable #1).
 * Un QR Code appartient à un utilisateur, possède un type contextuel,
 * une politique d'expiration et une limite de scans.
 * Invariants :
 * - L'owner est toujours défini
 * - Le type est toujours défini
 * - Les types à expiration obligatoire (TEMPORARY_LOCATION, EVENT) doivent avoir une date d'expiration
 * - Un QR révoqué ne peut plus être scanné
 * - Le compteur de scans ne peut pas dépasser la limite
 * - Un QR expiré ne peut pas être scanné
 */
public final class QrCode {

    private final QrCodeId id;
    private final UserId ownerId;
    private final QrCodeType type;
    private final ExpirationPolicy expirationPolicy;
    private final ScanLimit scanLimit;
    private final Instant createdAt;

    private int                 scanCount;
    private boolean             revoked;
    private Instant             revokedAt;
    private GeofenceRule        geofenceRule;
    private PersonalizationRule personalizationRule;

    private QrCode(
            QrCodeId id,
            UserId ownerId,
            QrCodeType type,
            ExpirationPolicy expirationPolicy,
            ScanLimit scanLimit,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.type = Objects.requireNonNull(type);
        this.expirationPolicy = Objects.requireNonNull(expirationPolicy);
        this.scanLimit = Objects.requireNonNull(scanLimit);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.scanCount           = 0;
        this.revoked             = false;
        this.revokedAt           = null;
        this.geofenceRule        = null;
        this.personalizationRule = null;
    }

    /**
     * Factory method : Crée un nouveau QR Code.
     * Applique la règle métier : certains types exigent une expiration.
     */
    public static QrCode create(
            UserId ownerId,
            QrCodeType type,
            ExpirationPolicy expirationPolicy,
            ScanLimit scanLimit,
            TimeProvider clock
    ) {
        Objects.requireNonNull(clock, "Le TimeProvider est obligatoire");

        // Règle métier : TEMPORARY_LOCATION et EVENT doivent avoir une expiration
        if (type.expirationIsMandatory() && expirationPolicy.isPermanent()) {
            throw new ExpirationRequiredForTypeException(
                "Le type " + type.displayName() + " requiert une date d'expiration"
            );
        }

        return new QrCode(
                QrCodeId.generate(),
                ownerId,
                type,
                expirationPolicy,
                scanLimit,
                clock.now()
        );
    }

    /**
     * Factory method : Reconstitue depuis la persistance.
     */
    public static QrCode reconstitute(
            QrCodeId id,
            UserId ownerId,
            QrCodeType type,
            ExpirationPolicy expirationPolicy,
            ScanLimit scanLimit,
            Instant createdAt,
            int scanCount,
            boolean revoked,
            Instant revokedAt
    ) {
        return reconstitute(id, ownerId, type, expirationPolicy, scanLimit, createdAt,
                            scanCount, revoked, revokedAt, null, null);
    }

    public static QrCode reconstitute(
            QrCodeId id,
            UserId ownerId,
            QrCodeType type,
            ExpirationPolicy expirationPolicy,
            ScanLimit scanLimit,
            Instant createdAt,
            int scanCount,
            boolean revoked,
            Instant revokedAt,
            GeofenceRule geofenceRule,
            PersonalizationRule personalizationRule
    ) {
        QrCode qrCode = new QrCode(id, ownerId, type, expirationPolicy, scanLimit, createdAt);
        qrCode.scanCount           = scanCount;
        qrCode.revoked             = revoked;
        qrCode.revokedAt           = revokedAt;
        qrCode.geofenceRule        = geofenceRule;
        qrCode.personalizationRule = personalizationRule;
        return qrCode;
    }

    // -------------------------------------------------------------------------
    // Comportements métier
    // -------------------------------------------------------------------------

    /**
     * Enregistre un scan du QR Code.
     * Vérifie toutes les conditions de validité avant d'accepter le scan.
     *
     * @param scannedAt L'instant du scan (fourni par TimeProvider)
     */
    public void recordScan(Instant scannedAt) {
        Objects.requireNonNull(scannedAt, "L'instant du scan est obligatoire");

        if (revoked) {
            throw new QrCodeRevokedException("Le QR Code " + id + " a été révoqué et ne peut plus être scanné");
        }
        if (expirationPolicy.isExpiredAt(scannedAt)) {
            throw new QrCodeExpiredException("Le QR Code " + id + " a expiré");
        }
        if (scanLimit.isReachedBy(scanCount)) {
            throw new ScanLimitReachedException(
                "Le QR Code " + id + " a atteint sa limite de scans (" + scanCount + ")"
            );
        }
        this.scanCount++;
    }

    /**
     * Révoque le QR Code immédiatement.
     * Idempotent : pas d'erreur si déjà révoqué.
     */
    public void revoke(TimeProvider clock) {
        Objects.requireNonNull(clock, "Le TimeProvider est obligatoire");
        if (!this.revoked) {
            this.revoked = true;
            this.revokedAt = clock.now();
        }
    }

    /**
     * Configure les règles de géofencing et personnalisation (F1.4).
     * Remplace les règles existantes.
     */
    public void configureRules(GeofenceRule geofenceRule, PersonalizationRule personalizationRule) {
        if (this.revoked) {
            throw new QrCodeRevokedException("Impossible de configurer les règles d'un QR Code révoqué");
        }
        this.geofenceRule        = geofenceRule;
        this.personalizationRule = personalizationRule;
    }

    /**
     * Vérifie si le QR Code est actuellement utilisable.
     */
    public boolean isActiveAt(Instant checkTime) {
        return !revoked
            && !expirationPolicy.isExpiredAt(checkTime)
            && !scanLimit.isReachedBy(scanCount);
    }

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    public QrCodeId id() { return id; }
    public UserId ownerId() { return ownerId; }
    public QrCodeType type() { return type; }
    public ExpirationPolicy expirationPolicy() { return expirationPolicy; }
    public ScanLimit scanLimit() { return scanLimit; }
    public Instant createdAt() { return createdAt; }
    public int scanCount() { return scanCount; }
    public boolean isRevoked() { return revoked; }
    public java.util.Optional<Instant>             revokedAt()             { return java.util.Optional.ofNullable(revokedAt); }
    public java.util.Optional<GeofenceRule>        geofenceRule()          { return java.util.Optional.ofNullable(geofenceRule); }
    public java.util.Optional<PersonalizationRule> personalizationRule()   { return java.util.Optional.ofNullable(personalizationRule); }
    public boolean hasGeofence() { return geofenceRule != null; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QrCode q)) return false;
        return id.equals(q.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    // -------------------------------------------------------------------------
    // Exceptions de domaine
    // -------------------------------------------------------------------------

    public static final class QrCodeRevokedException extends DomainException {
        public QrCodeRevokedException(String m) { super(m); }
    }

    public static final class QrCodeExpiredException extends DomainException {
        public QrCodeExpiredException(String m) { super(m); }
    }

    public static final class ScanLimitReachedException extends DomainException {
        public ScanLimitReachedException(String m) { super(m); }
    }

    public static final class ExpirationRequiredForTypeException extends DomainException {
        public ExpirationRequiredForTypeException(String m) { super(m); }
    }
}
