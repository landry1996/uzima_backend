package com.uzima.application.qrcode.port.in;

import com.uzima.domain.qrcode.model.GeofenceRule;
import com.uzima.domain.qrcode.model.PersonalizationRule;
import com.uzima.domain.qrcode.model.QrCodeId;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/** Commande : Configurer les règles contextuelles d'un QR Code. */
public record ConfigureQrCodeRulesCommand(
        QrCodeId            qrCodeId,
        UserId              requesterId,
        GeofenceRule        geofenceRule,        // nullable
        PersonalizationRule personalizationRule  // nullable
) {
    public ConfigureQrCodeRulesCommand {
        Objects.requireNonNull(qrCodeId,    "qrCodeId est obligatoire");
        Objects.requireNonNull(requesterId, "requesterId est obligatoire");
    }
}
