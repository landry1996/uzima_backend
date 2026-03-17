package com.uzima.domain.qrcode.specification;

import com.uzima.domain.qrcode.model.QrCode;
import com.uzima.domain.shared.specification.Specification;

/**
 * Specification atomique : vérifie qu'un QR Code est révoqué.
 * Usage dans la composition :
 *   Specification<QrCode> notRevoked = new QrCodeRevokedSpecification().not();
 * Usage direct : requêtes d'audit ou d'archivage des QR Codes révoqués.
 *   qrCodes.stream().filter(new QrCodeRevokedSpecification()::isSatisfiedBy)
 */
public final class QrCodeRevokedSpecification implements Specification<QrCode> {

    @Override
    public boolean isSatisfiedBy(QrCode qrCode) {
        return qrCode.isRevoked();
    }
}
