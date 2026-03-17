package com.uzima.application.qrcode.port.out;

import com.uzima.domain.qrcode.port.QrCodeRepository;

/**
 * Port de sortie (couche application) : persistance des QR Codes.
 * <p>
 * Étend le port du domaine (QrCodeRepository) sans duplication.
 * Méthodes héritées :
 * - save(QrCode)
 * - findById(QrCodeId)
 * - findByOwnerId(UserId)  → utilisé par GetMyQrCodesUseCase
 * - delete(QrCodeId)       → utilisé pour la suppression RGPD (erasure)
 * <p>
 * Méthodes applicatives spécifiques à ajouter ici si besoin (ex: findByType, pagination).
 */
public interface QrCodeRepositoryPort extends QrCodeRepository {
}
