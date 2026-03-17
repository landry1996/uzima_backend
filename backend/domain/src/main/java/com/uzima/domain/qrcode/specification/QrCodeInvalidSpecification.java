package com.uzima.domain.qrcode.specification;

import com.uzima.domain.qrcode.model.QrCode;
import com.uzima.domain.shared.specification.Specification;

import java.time.Instant;
import java.util.Objects;

/**
 * Specification composite : vérifie qu'un QR Code est invalide.
 * Un QR Code est invalide s'il est révoqué OU expiré OU a atteint sa limite de scans.
 * Démontre la composition par or() du pattern Specification :
 *   revoked.or(expired).or(limitReached)
 * Inverse logique de QrCodeIsActiveSpecification.
 * Usage : sélection des QR Codes à archiver lors des jobs de maintenance.
 *   Specification<QrCode> invalid = new QrCodeInvalidSpecification(clock.now());
 *   List<QrCode> toArchive = qrCodes.stream()
 *           .filter(invalid::isSatisfiedBy)
 *           .toList();
 */
public final class QrCodeInvalidSpecification implements Specification<QrCode> {

    private final Specification<QrCode> composed;

    public QrCodeInvalidSpecification(Instant checkTime) {
        Objects.requireNonNull(checkTime, "L'instant de vérification est obligatoire");

        Specification<QrCode> revoked      = new QrCodeRevokedSpecification();
        Specification<QrCode> expired      = new QrCodeExpiredSpecification(checkTime);
        Specification<QrCode> limitReached = new QrCodeScanLimitReachedSpecification();

        // Composition par or() : invalide = révoqué OU expiré OU limite atteinte
        this.composed = revoked.or(expired).or(limitReached);
    }

    @Override
    public boolean isSatisfiedBy(QrCode qrCode) {
        return composed.isSatisfiedBy(qrCode);
    }
}
