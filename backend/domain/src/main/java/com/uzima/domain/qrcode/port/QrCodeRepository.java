package com.uzima.domain.qrcode.port;

import com.uzima.domain.qrcode.model.QrCode;
import com.uzima.domain.qrcode.model.QrCodeId;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Port de sortie : Accès à la persistance des QR Codes.
 * Appartient au domaine. Implémenté par l'infrastructure.
 */
public interface QrCodeRepository {

    void save(QrCode qrCode);

    Optional<QrCode> findById(QrCodeId id);

    List<QrCode> findByOwnerId(UserId ownerId);

    void delete(QrCodeId id);
}
