package com.uzima.domain.qrcode.service;

import com.uzima.domain.qrcode.model.QrCode;
import com.uzima.domain.qrcode.model.QrCodeId;
import com.uzima.domain.qrcode.port.QrCodeRepository;
import com.uzima.domain.shared.DomainException;
import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Service de domaine : opérations QR Code nécessitant le port de persistance.
 * Justification de ce service :
 * Un Aggregate Root ne peut pas dépendre d'un port (repository) — règle DDD.
 * Ce service encapsule les opérations qui nécessitent QrCodeRepository
 * sans porter de logique applicative.
 * Méthodes utilisées du domaine :
 * - QrCode.reconstitute() : construit via findById() dans l'adaptateur infrastructure
 * - QrCode.createdAt() : date de création pour le calcul du cycle de vie
 * - ExpirationPolicy.expiresAt() : date d'expiration pour le résumé de lifecycle
 * - QrCodeRepository.delete() : suppression physique (droit à l'effacement RGPD)
 */
public final class QrCodeDomainService {

    private final QrCodeRepository qrCodeRepository;

    public QrCodeDomainService(QrCodeRepository qrCodeRepository) {
        this.qrCodeRepository = Objects.requireNonNull(qrCodeRepository,
                "QrCodeRepository est obligatoire");
    }

    /**
     * Supprime un QR Code si et seulement s'il appartient au demandeur.
     * Implémente le droit à l'effacement (RGPD Article 17).
     * Le QrCode est reconstitué depuis la persistance via QrCode.reconstitute()
     * dans l'adaptateur infrastructure (appelé par findById()).
     * Utilise QrCodeRepository.delete() après vérification de l'ownership.
     *
     * @param qrCodeId        L'identifiant du QR Code à supprimer
     * @param requestingOwner L'utilisateur demandant la suppression
     * @throws QrCodeNotFoundException          si le QR Code n'existe pas
     * @throws QrCodeOwnershipViolationException si l'utilisateur n'en est pas propriétaire
     */
    public void deleteIfOwned(QrCodeId qrCodeId, UserId requestingOwner) {
        // findById() retourne un QrCode reconstitué via QrCode.reconstitute() dans l'adaptateur
        QrCode qrCode = qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new QrCodeNotFoundException(
                        "QR Code introuvable : " + qrCodeId));

        if (!qrCode.ownerId().equals(requestingOwner)) {
            throw new QrCodeOwnershipViolationException(
                    "L'utilisateur " + requestingOwner
                    + " n'est pas propriétaire du QR Code " + qrCodeId);
        }

        qrCodeRepository.delete(qrCodeId);
    }

    /**
     * Retourne le résumé du cycle de vie d'un QR Code.
     * Utilise :
     * - QrCode.createdAt() : date de création (fenêtre de validité début)
     * - QrCode.expirationPolicy().expiresAt() : date d'expiration si définie (fenêtre fin)
     *
     * @param qrCodeId L'identifiant du QR Code
     * @return Résumé immuable du cycle de vie
     * @throws QrCodeNotFoundException si le QR Code n'existe pas
     */
    public LifecycleSummary getLifecycleSummary(QrCodeId qrCodeId) {
        QrCode qrCode = qrCodeRepository.findById(qrCodeId)
                .orElseThrow(() -> new QrCodeNotFoundException(
                        "QR Code introuvable : " + qrCodeId));

        // expiresAt() retourne Optional<Instant> — permanent si vide
        Optional<Instant> expiresAt = qrCode.expirationPolicy().expiresAt();

        return new LifecycleSummary(
                qrCode.id(),
                qrCode.ownerId(),
                qrCode.createdAt(),
                expiresAt,
                qrCode.isRevoked()
        );
    }

    // -------------------------------------------------------------------------
    // Types de retour
    // -------------------------------------------------------------------------

    /**
     * Vue immuable du cycle de vie d'un QR Code.
     * createdAt() et expiresAt() délimitent la fenêtre de validité temporelle.
     */
    public record LifecycleSummary(
            QrCodeId id,
            UserId ownerId,
            Instant createdAt,
            Optional<Instant> expiresAt,
            boolean revoked
    ) {
        /** Retourne true si le QR Code a une expiration définie. */
        public boolean hasExpiration() {
            return expiresAt.isPresent();
        }
    }

    // -------------------------------------------------------------------------
    // Exceptions de domaine
    // -------------------------------------------------------------------------

    public static final class QrCodeNotFoundException extends DomainException {
        public QrCodeNotFoundException(String m) { super(m); }
    }

    public static final class QrCodeOwnershipViolationException extends DomainException {
        public QrCodeOwnershipViolationException(String m) { super(m); }
    }
}
