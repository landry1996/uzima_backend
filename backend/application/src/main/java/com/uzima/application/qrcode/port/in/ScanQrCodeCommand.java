package com.uzima.application.qrcode.port.in;

import com.uzima.domain.qrcode.model.QrCodeId;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/** Commande : Scanner un QR Code. */
public record ScanQrCodeCommand(
        QrCodeId qrCodeId,
        UserId   scannerId
) {
    public ScanQrCodeCommand {
        Objects.requireNonNull(qrCodeId,  "qrCodeId est obligatoire");
        Objects.requireNonNull(scannerId, "scannerId est obligatoire");
    }
}
