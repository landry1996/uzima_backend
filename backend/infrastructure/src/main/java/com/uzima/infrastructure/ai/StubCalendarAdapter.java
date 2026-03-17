package com.uzima.infrastructure.ai;

import com.uzima.application.qrcode.port.out.CalendarIntegrationPort;
import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.Optional;

/**
 * Adaptateur stub : Calendrier no-op (pour dev/test).
 * <p>
 * Retourne toujours empty — pas d'événement actif en dev.
 * Remplacer par un adaptateur Google Calendar / iCal en production.
 */
public class StubCalendarAdapter implements CalendarIntegrationPort {

    @Override
    public Optional<CalendarEvent> getActiveEvent(UserId userId, Instant at) {
        return Optional.empty();
    }
}
