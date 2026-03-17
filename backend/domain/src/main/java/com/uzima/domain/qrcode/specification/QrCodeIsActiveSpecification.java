package com.uzima.domain.qrcode.specification;

import com.uzima.domain.qrcode.model.QrCode;
import com.uzima.domain.shared.specification.Specification;

import java.time.Instant;
import java.util.Objects;

/**
 * Specification composite : vérifie qu'un QR Code est actif au moment du scan.
 * Construit par composition via not() et and() à partir des trois specs atomiques :
 *   notRevoked.and(notExpired).and(notScanLimitReached)
 * Avantages de cette composition :
 * - Chaque règle atomique est testable indépendamment
 * - Lisibilité : chaque terme est nommé explicitement
 * - Extensibilité : ajouter une règle = ajouter un .and(newSpec)
 * Usage : vérification avant qrCode.recordScan() pour retourner un message précis.
 *   var spec = new QrCodeIsActiveSpecification(clock.now());
 *   if (!spec.isSatisfiedBy(qrCode)) {
 *       throw new QrCodeInactiveException(spec.failureReason(qrCode));
 *   }
 */
public final class QrCodeIsActiveSpecification implements Specification<QrCode> {

    private final Instant checkTime;
    private final Specification<QrCode> composed;

    public QrCodeIsActiveSpecification(Instant checkTime) {
        this.checkTime = Objects.requireNonNull(checkTime, "L'instant de vérification est obligatoire");

        // Composition via not() + and() des spécifications atomiques
        Specification<QrCode> notRevoked       = new QrCodeRevokedSpecification().not();
        Specification<QrCode> notExpired       = new QrCodeExpiredSpecification(checkTime).not();
        Specification<QrCode> notLimitReached  = new QrCodeScanLimitReachedSpecification().not();

        this.composed = notRevoked.and(notExpired).and(notLimitReached);
    }

    @Override
    public boolean isSatisfiedBy(QrCode qrCode) {
        return composed.isSatisfiedBy(qrCode);
    }

    /**
     * Retourne la raison d'échec pour un message d'erreur précis (logs internes).
     * Délègue aux spécifications atomiques pour éviter la duplication de logique.
     */
    public String failureReason(QrCode qrCode) {
        if (new QrCodeRevokedSpecification().isSatisfiedBy(qrCode))       return "QR_CODE_REVOKED";
        if (new QrCodeExpiredSpecification(checkTime).isSatisfiedBy(qrCode)) return "QR_CODE_EXPIRED";
        if (new QrCodeScanLimitReachedSpecification().isSatisfiedBy(qrCode)) return "SCAN_LIMIT_REACHED";
        return "UNKNOWN";
    }
}
