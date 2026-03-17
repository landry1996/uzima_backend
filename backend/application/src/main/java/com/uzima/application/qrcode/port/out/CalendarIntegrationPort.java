package com.uzima.application.qrcode.port.out;

import com.uzima.domain.user.model.UserId;

import java.time.Instant;

/** Port OUT : Intégration calendrier (Google Calendar, iCal…). */
public interface CalendarIntegrationPort {

    /**
     * Vérifie si l'utilisateur a un événement actif à l'instant donné.
     *
     * @param userId Identifiant de l'utilisateur
     * @param at     Instant à vérifier
     * @return Informations sur l'événement actif, ou absent
     */
    java.util.Optional<CalendarEvent> getActiveEvent(UserId userId, Instant at);

    record CalendarEvent(String title, String category, Instant startAt, Instant endAt) {
        public boolean isWorkEvent() {
            return category != null && category.equalsIgnoreCase("WORK");
        }
    }
}
