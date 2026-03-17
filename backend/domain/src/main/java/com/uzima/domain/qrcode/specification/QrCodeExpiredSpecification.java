package com.uzima.domain.qrcode.specification;

import com.uzima.domain.qrcode.model.QrCode;
import com.uzima.domain.shared.specification.Specification;

import java.time.Instant;
import java.util.Objects;

/**
 * Specification atomique : vérifie qu'un QR Code est expiré à un instant donné.
 * Usage dans la composition :
 *   Specification<QrCode> notExpired = new QrCodeExpiredSpecification(now).not();
 * Usage direct : sélection des QR Codes expirés pour les jobs de nettoyage.
 *   qrCodes.stream().filter(new QrCodeExpiredSpecification(clock.now())::isSatisfiedBy)
 */
public final class QrCodeExpiredSpecification implements Specification<QrCode> {

    private final Instant checkTime;

    public QrCodeExpiredSpecification(Instant checkTime) {
        this.checkTime = Objects.requireNonNull(checkTime, "L'instant de vérification est obligatoire");
    }

    @Override
    public boolean isSatisfiedBy(QrCode qrCode) {
        return qrCode.expirationPolicy().isExpiredAt(checkTime);
    }
}
