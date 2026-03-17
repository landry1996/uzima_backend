package com.uzima.infrastructure.persistence.assistant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reminders")
@Getter
@NoArgsConstructor
public class ReminderJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private String trigger;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private String status;

    @Column(name = "triggered_at")
    private Instant triggeredAt;

    @Column(name = "dismissed_at")
    private Instant dismissedAt;

    @Column(name = "snoozed_until")
    private Instant snoozedUntil;

    public static ReminderJpaEntity of(UUID id, UUID userId, String content, String trigger,
                                        Instant scheduledAt, Instant createdAt, String status,
                                        Instant triggeredAt, Instant dismissedAt, Instant snoozedUntil) {
        ReminderJpaEntity e = new ReminderJpaEntity();
        e.id           = id;
        e.userId       = userId;
        e.content      = content;
        e.trigger      = trigger;
        e.scheduledAt  = scheduledAt;
        e.createdAt    = createdAt;
        e.status       = status;
        e.triggeredAt  = triggeredAt;
        e.dismissedAt  = dismissedAt;
        e.snoozedUntil = snoozedUntil;
        return e;
    }
}
