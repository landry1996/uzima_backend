package com.uzima.domain.qrcode.specification;

import com.uzima.domain.qrcode.model.QrCode;
import com.uzima.domain.shared.specification.Specification;

/**
 * Specification atomique : vérifie que la limite de scans d'un QR Code est atteinte.
 * Usage dans la composition :
 *   Specification<QrCode> notLimitReached = new QrCodeScanLimitReachedSpecification().not();
 * Usage direct : statistiques d'utilisation ou sélection des QR Codes saturés.
 *   qrCodes.stream().filter(new QrCodeScanLimitReachedSpecification()::isSatisfiedBy)
 */
public final class QrCodeScanLimitReachedSpecification implements Specification<QrCode> {

    @Override
    public boolean isSatisfiedBy(QrCode qrCode) {
        return qrCode.scanLimit().isReachedBy(qrCode.scanCount());
    }
}
