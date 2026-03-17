package com.uzima.application.qrcode.port.in;

import com.uzima.domain.qrcode.model.QrCodeType;
import com.uzima.domain.user.model.UserId;

import java.time.Duration;
import java.util.Objects;

/**
 * Commande d'entrée : Création d'un QR Code contextuel.
 *
 * Les champs optionnels (validFor, singleUsePayment) ne s'appliquent
 * qu'à certains types — la validation fine est déléguée à QrCodeFactory.
 */
public record CreateQrCodeCommand(
        UserId ownerId,
        QrCodeType type,
        Duration validFor,       // Requis pour TEMPORARY_LOCATION et EVENT, ignoré sinon
        boolean singleUsePayment // Pertinent uniquement pour PAYMENT
) {
    public CreateQrCodeCommand {
        Objects.requireNonNull(ownerId, "Le propriétaire est obligatoire");
        Objects.requireNonNull(type, "Le type de QR Code est obligatoire");
    }
}
