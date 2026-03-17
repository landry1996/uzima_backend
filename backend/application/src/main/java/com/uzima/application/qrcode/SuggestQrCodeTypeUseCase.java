package com.uzima.application.qrcode;

import com.uzima.application.qrcode.port.out.CalendarIntegrationPort;
import com.uzima.application.qrcode.port.out.GeolocationPort;
import com.uzima.domain.qrcode.model.QrCodeType;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Use Case : Suggérer le type de QR Code adapté au contexte (F1.3).
 * <p>
 * Analyse la géolocalisation et le calendrier de l'utilisateur
 * pour recommander le type le plus pertinent.
 */
public class SuggestQrCodeTypeUseCase {

    private final GeolocationPort       geolocationPort;
    private final CalendarIntegrationPort calendarPort;
    private final TimeProvider          clock;

    public SuggestQrCodeTypeUseCase(GeolocationPort geolocationPort,
                                     CalendarIntegrationPort calendarPort,
                                     TimeProvider clock) {
        this.geolocationPort = Objects.requireNonNull(geolocationPort);
        this.calendarPort    = Objects.requireNonNull(calendarPort);
        this.clock           = Objects.requireNonNull(clock);
    }

    public Suggestion execute(UserId userId) {
        Objects.requireNonNull(userId, "userId est obligatoire");

        var now = clock.now();

        // Vérifier événement calendrier actif
        var activeEvent = calendarPort.getActiveEvent(userId, now);
        if (activeEvent.isPresent()) {
            var event = activeEvent.get();
            if (event.isWorkEvent()) {
                return new Suggestion(QrCodeType.PROFESSIONAL, "Événement professionnel actif : " + event.title());
            }
            return new Suggestion(QrCodeType.EVENT, "Événement actif : " + event.title());
        }

        // Sinon : suggérer selon l'heure
        var hour = java.time.ZonedDateTime.ofInstant(now, java.time.ZoneOffset.UTC).getHour();
        if (hour >= 8 && hour < 18) {
            return new Suggestion(QrCodeType.PROFESSIONAL, "Heure de travail — réseau professionnel recommandé");
        }
        return new Suggestion(QrCodeType.SOCIAL, "Hors horaires de travail — réseau social recommandé");
    }

    public record Suggestion(QrCodeType suggestedType, String reason) {}
}
