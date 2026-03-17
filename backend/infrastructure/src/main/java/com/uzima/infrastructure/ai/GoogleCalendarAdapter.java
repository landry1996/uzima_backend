package com.uzima.infrastructure.ai;

import com.uzima.application.qrcode.port.out.CalendarIntegrationPort;
import com.uzima.domain.user.model.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;

/**
 * Adaptateur production : Intégration Google Calendar.
 * <p>
 * L'intégration Google Calendar nécessite une configuration OAuth2 par utilisateur
 * (client_id, client_secret, refresh_token stocké par user).
 * <p>
 * Cette implémentation retourne {@link Optional#empty()} jusqu'à ce que
 * le flux OAuth2 Google soit implémenté.
 * <p>
 * Pour activer cette fonctionnalité :
 * <ol>
 *   <li>Configurer {@code uzima.integrations.google.client-id} et {@code client-secret}</li>
 *   <li>Implémenter le flux OAuth2 pour stocker le refresh_token par utilisateur</li>
 *   <li>Appeler {@code https://www.googleapis.com/calendar/v3/calendars/primary/events}</li>
 * </ol>
 */
public class GoogleCalendarAdapter implements CalendarIntegrationPort {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarAdapter.class);

    @Override
    public Optional<CalendarEvent> getActiveEvent(UserId userId, Instant at) {
        log.debug("[CALENDAR] Intégration Google Calendar non configurée pour l'utilisateur {} — "
                + "pas d'événement retourné", userId);
        return Optional.empty();
    }
}
