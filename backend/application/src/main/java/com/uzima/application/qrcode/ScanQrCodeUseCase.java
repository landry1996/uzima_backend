package com.uzima.application.qrcode;

import com.uzima.application.qrcode.port.in.ScanQrCodeCommand;
import com.uzima.application.qrcode.port.out.GeolocationPort;
import com.uzima.application.qrcode.port.out.QrCodeRepositoryPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.domain.qrcode.model.QrCode;
import com.uzima.domain.qrcode.model.QrCodeType;
import com.uzima.domain.shared.TimeProvider;

import java.util.Objects;

/**
 * Use Case : Scanner un QR Code (F1.6).
 * <p>
 * Valide les conditions (actif, géofencing si applicable),
 * incrémente le compteur de scans et retourne le résultat.
 */
public class ScanQrCodeUseCase {

    private final QrCodeRepositoryPort repository;
    private final TimeProvider         clock;
    private final GeolocationPort      geolocationPort;

    public ScanQrCodeUseCase(QrCodeRepositoryPort repository, TimeProvider clock,
                              GeolocationPort geolocationPort) {
        this.repository      = Objects.requireNonNull(repository);
        this.clock           = Objects.requireNonNull(clock);
        this.geolocationPort = Objects.requireNonNull(geolocationPort);
    }

    public ScanResult execute(ScanQrCodeCommand cmd) {
        Objects.requireNonNull(cmd, "La commande est obligatoire");

        QrCode qrCode = repository.findById(cmd.qrCodeId())
            .orElseThrow(() -> ResourceNotFoundException.qrCodeNotFound(cmd.qrCodeId()));

        // Vérification géofencing si défini (F1.3)
        if (qrCode.hasGeofence()) {
            var geofence = qrCode.geofenceRule().get();
            var location = geolocationPort.getCurrentLocation(cmd.scannerId())
                .orElseThrow(() -> new GeolocationUnavailableException(
                    "La géolocalisation est requise pour scanner ce QR Code"
                ));
            if (!geofence.contains(location.latitude(), location.longitude())) {
                throw new OutsideGeofenceException(
                    "Vous n'êtes pas dans la zone autorisée pour scanner ce QR Code"
                );
            }
        }

        qrCode.recordScan(clock.now());
        repository.save(qrCode);

        return new ScanResult(
            qrCode.id().toString(),
            qrCode.type(),
            qrCode.ownerId().toString(),
            qrCode.scanCount()
        );
    }

    public record ScanResult(String qrCodeId, QrCodeType type, String ownerId, int totalScans) {}

    public static final class GeolocationUnavailableException extends RuntimeException {
        public GeolocationUnavailableException(String message) { super(message); }
    }

    public static final class OutsideGeofenceException extends RuntimeException {
        public OutsideGeofenceException(String message) { super(message); }
    }
}
