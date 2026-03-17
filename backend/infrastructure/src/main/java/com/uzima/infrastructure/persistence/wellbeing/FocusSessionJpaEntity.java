package com.uzima.infrastructure.persistence.wellbeing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "focus_sessions")
@Getter
@NoArgsConstructor
public class FocusSessionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private String status;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "interruption_reason")
    private String interruptionReason;

    public static FocusSessionJpaEntity of(UUID id, UUID userId, Instant startedAt,
                                            String status, Instant endedAt,
                                            String interruptionReason) {
        FocusSessionJpaEntity e = new FocusSessionJpaEntity();
        e.id                 = id;
        e.userId             = userId;
        e.startedAt          = startedAt;
        e.status             = status;
        e.endedAt            = endedAt;
        e.interruptionReason = interruptionReason;
        return e;
    }
}
