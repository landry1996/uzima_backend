package com.uzima.bootstrap.adapter.http.dto;

import com.uzima.domain.assistant.model.Reminder;

public record ReminderResponse(
            String id, String userId, String content, String trigger,
            String status, String scheduledAt, String triggeredAt,
            String dismissedAt, String snoozedUntil
    ) {
        public static ReminderResponse from(Reminder r) {
            return new ReminderResponse(
                r.id().toString(), r.userId().toString(), r.content(),
                r.trigger().name(), r.status().name(),
                r.scheduledAt().toString(),
                r.triggeredAt().map(Object::toString).orElse(null),
                r.dismissedAt().map(Object::toString).orElse(null),
                r.snoozedUntil().map(Object::toString).orElse(null)
            );
        }
    }