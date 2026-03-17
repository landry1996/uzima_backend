package com.uzima.application.qrcode;

import com.uzima.application.qrcode.port.in.ConfigureQrCodeRulesCommand;
import com.uzima.application.qrcode.port.out.QrCodeRepositoryPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.domain.qrcode.model.QrCode;

import java.util.Objects;

/** Use Case : Configurer les règles de géofencing et personnalisation d'un QR Code (F1.4). */
public class ConfigureQrCodeRulesUseCase {

    private final QrCodeRepositoryPort repository;

    public ConfigureQrCodeRulesUseCase(QrCodeRepositoryPort repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public void execute(ConfigureQrCodeRulesCommand cmd) {
        Objects.requireNonNull(cmd, "La commande est obligatoire");

        QrCode qrCode = repository.findById(cmd.qrCodeId())
            .orElseThrow(() -> ResourceNotFoundException.qrCodeNotFound(cmd.qrCodeId()));

        if (!qrCode.ownerId().equals(cmd.requesterId())) {
            throw UnauthorizedException.cannotEditOthersQrCode(cmd.qrCodeId());
        }

        qrCode.configureRules(cmd.geofenceRule(), cmd.personalizationRule());
        repository.save(qrCode);
    }
}
