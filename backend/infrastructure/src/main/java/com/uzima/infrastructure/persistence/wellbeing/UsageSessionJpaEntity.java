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
@Table(name = "usage_sessions")
@Getter
@NoArgsConstructor
public class UsageSessionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "app_name", nullable = false, length = 100)
    private String appName;

    @Column(name = "app_type", nullable = false)
    private String appType;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    public static UsageSessionJpaEntity of(UUID id, UUID userId, String appName, String appType,
                                            Instant startedAt, Instant endedAt) {
        UsageSessionJpaEntity e = new UsageSessionJpaEntity();
        e.id        = id;
        e.userId    = userId;
        e.appName   = appName;
        e.appType   = appType;
        e.startedAt = startedAt;
        e.endedAt   = endedAt;
        return e;
    }
}
