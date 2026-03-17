package com.uzima.domain.qrcode.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object : Identifiant unique d'un QR Code.
 */
public record QrCodeId(UUID value) {

    public QrCodeId {
        Objects.requireNonNull(value, "L'identifiant du QR Code ne peut pas être nul");
    }

    public static QrCodeId generate() {
        return new QrCodeId(UUID.randomUUID());
    }

    public static QrCodeId of(UUID uuid) {
        return new QrCodeId(uuid);
    }

    public static QrCodeId of(String uuid) {
        return new QrCodeId(UUID.fromString(uuid));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
