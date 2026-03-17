package com.uzima.domain.qrcode.factory;

import com.uzima.domain.qrcode.model.*;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.time.Duration;
import java.util.Objects;

/**
 * Factory métier : création de QR Codes contextuels (Innovation Brevetable #1).
 * Justification du pattern Factory (fortement justifiée ici) :
 * Chaque type de QR Code a des règles de création différentes :
 * - PROFESSIONAL : permanent, scans illimités par défaut
 * - SOCIAL : permanent, scans illimités par défaut
 * - PAYMENT : permanent, scans illimités, peut être limité à 1 scan
 * - TEMPORARY_LOCATION : expiration OBLIGATOIRE (règle métier), 1 scan max par défaut
 * - EVENT : expiration OBLIGATOIRE, scans illimités
 * - MEDICAL_EMERGENCY : permanent (must work offline), scans illimités
 * Sans factory, chaque appelant devrait :
 * 1. Connaître les règles par type
 * 2. Construire ExpirationPolicy et ScanLimit manuellement
 * 3. Risquer de violer les invariants (ex: TEMPORARY_LOCATION sans expiration)
 * La factory centralise ces règles et REND IMPOSSIBLE la création incorrecte.
 */
public final class QrCodeFactory {

    private QrCodeFactory() {
        // Classe utilitaire statique
    }

    /**
     * Crée un QR Code PROFESSIONNEL (permanent, illimité).
     */
    public static QrCode createProfessional(UserId ownerId, TimeProvider clock) {
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(clock);
        return QrCode.create(
                ownerId,
                QrCodeType.PROFESSIONAL,
                ExpirationPolicy.permanent(),
                ScanLimit.unlimited(),
                clock
        );
    }

    /**
     * Crée un QR Code SOCIAL (permanent, illimité).
     */
    public static QrCode createSocial(UserId ownerId, TimeProvider clock) {
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(clock);
        return QrCode.create(
                ownerId,
                QrCodeType.SOCIAL,
                ExpirationPolicy.permanent(),
                ScanLimit.unlimited(),
                clock
        );
    }

    /**
     * Crée un QR Code PAIEMENT.
     *
     * @param singleUse Si true, le QR ne peut être scanné qu'une seule fois
     *                  (ex: paiement unique pour une facture précise)
     */
    public static QrCode createPayment(UserId ownerId, boolean singleUse, TimeProvider clock) {
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(clock);
        ScanLimit limit = singleUse ? ScanLimit.of(1) : ScanLimit.unlimited();
        return QrCode.create(
                ownerId,
                QrCodeType.PAYMENT,
                ExpirationPolicy.permanent(),
                limit,
                clock
        );
    }

    /**
     * Crée un QR Code LOCALISATION TEMPORAIRE.
     * Règle métier : expiration OBLIGATOIRE (entre 30min et 48h selon le cahier des charges).
     * Par sécurité, 1 scan maximum par défaut pour éviter le tracking continu.
     *
     * @param validFor Durée de validité (ex: Duration.ofHours(2))
     */
    public static QrCode createTemporaryLocation(UserId ownerId, Duration validFor, TimeProvider clock) {
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(validFor);
        Objects.requireNonNull(clock);
        validateLocationDuration(validFor);

        return QrCode.create(
                ownerId,
                QrCodeType.TEMPORARY_LOCATION,
                ExpirationPolicy.expiresAfter(validFor, clock.now()),
                ScanLimit.unlimited(), // scans multiples autorisés (traçabilité livraison)
                clock
        );
    }

    /**
     * Crée un QR Code ÉVÉNEMENT.
     * Règle métier : expiration OBLIGATOIRE, doit être après la fin de l'événement.
     *
     * @param validFor Durée de validité (ex: Duration.ofDays(1) pour une journée conférence)
     */
    public static QrCode createEvent(UserId ownerId, Duration validFor, TimeProvider clock) {
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(validFor);
        Objects.requireNonNull(clock);

        return QrCode.create(
                ownerId,
                QrCodeType.EVENT,
                ExpirationPolicy.expiresAfter(validFor, clock.now()),
                ScanLimit.unlimited(),
                clock
        );
    }

    /**
     * Crée un QR Code URGENCE MÉDICALE.
     * Règle métier :
     * - Permanent (doit fonctionner même si l'utilisateur est inconscient)
     * - Scans illimités (plusieurs intervenants peuvent scanner)
     * - Supporte le mode offline (données encodées dans le QR lui-même)
     */
    public static QrCode createMedicalEmergency(UserId ownerId, TimeProvider clock) {
        Objects.requireNonNull(ownerId);
        Objects.requireNonNull(clock);
        return QrCode.create(
                ownerId,
                QrCodeType.MEDICAL_EMERGENCY,
                ExpirationPolicy.permanent(),
                ScanLimit.unlimited(),
                clock
        );
    }

    // -------------------------------------------------------------------------
    // Validation interne
    // -------------------------------------------------------------------------

    private static void validateLocationDuration(Duration duration) {
        Duration min = Duration.ofMinutes(30);
        Duration max = Duration.ofHours(48);
        if (duration.compareTo(min) < 0) {
            throw new IllegalArgumentException(
                "La durée de localisation temporaire doit être d'au moins 30 minutes"
            );
        }
        if (duration.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                "La durée de localisation temporaire ne peut pas dépasser 48 heures"
            );
        }
    }
}
