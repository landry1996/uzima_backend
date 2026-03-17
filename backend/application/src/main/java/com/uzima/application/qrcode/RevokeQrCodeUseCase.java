package com.uzima.application.qrcode;

import com.uzima.application.qrcode.port.out.QrCodeRepositoryPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.domain.qrcode.model.QrCode;
import com.uzima.domain.qrcode.model.QrCodeId;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/** Use Case : Révoquer un QR Code. */
public class RevokeQrCodeUseCase {

    private final QrCodeRepositoryPort repository;
    private final TimeProvider         clock;

    public RevokeQrCodeUseCase(QrCodeRepositoryPort repository, TimeProvider clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock      = Objects.requireNonNull(clock);
    }

    public void execute(QrCodeId qrCodeId, UserId requesterId) {
        Objects.requireNonNull(qrCodeId,    "qrCodeId est obligatoire");
        Objects.requireNonNull(requesterId, "requesterId est obligatoire");

        QrCode qrCode = repository.findById(qrCodeId)
            .orElseThrow(() -> ResourceNotFoundException.qrCodeNotFound(qrCodeId));

        if (!qrCode.ownerId().equals(requesterId)) {
            throw UnauthorizedException.cannotRevokeOthersQrCode(qrCodeId);
        }

        qrCode.revoke(clock);
        repository.save(qrCode);
    }
}
