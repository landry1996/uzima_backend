package com.uzima.infrastructure.persistence.assistant;

import com.uzima.domain.assistant.model.Reminder;
import com.uzima.domain.assistant.model.ReminderId;
import com.uzima.domain.assistant.model.ReminderStatus;
import com.uzima.domain.assistant.model.ReminderTrigger;
import com.uzima.domain.user.model.UserId;

class ReminderEntityMapper {

    Reminder toDomain(ReminderJpaEntity e) {
        return Reminder.reconstitute(
            ReminderId.of(e.getId()),
            UserId.of(e.getUserId()),
            e.getContent(),
            ReminderTrigger.valueOf(e.getTrigger()),
            e.getScheduledAt(),
            e.getCreatedAt(),
            ReminderStatus.valueOf(e.getStatus()),
            e.getTriggeredAt(),
            e.getDismissedAt(),
            e.getSnoozedUntil()
        );
    }

    ReminderJpaEntity toJpaEntity(Reminder r) {
        return ReminderJpaEntity.of(
            r.id().value(),
            r.userId().value(),
            r.content(),
            r.trigger().name(),
            r.scheduledAt(),
            r.createdAt(),
            r.status().name(),
            r.triggeredAt().orElse(null),
            r.dismissedAt().orElse(null),
            r.snoozedUntil().orElse(null)
        );
    }
}
