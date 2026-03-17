package com.uzima.infrastructure.persistence.wellbeing;

import com.uzima.domain.user.model.UserId;
import com.uzima.domain.wellbeing.model.AppType;
import com.uzima.domain.wellbeing.model.FocusSession;
import com.uzima.domain.wellbeing.model.FocusSessionId;
import com.uzima.domain.wellbeing.model.FocusSessionStatus;
import com.uzima.domain.wellbeing.model.InterruptionReason;
import com.uzima.domain.wellbeing.model.UsageSession;
import com.uzima.domain.wellbeing.model.UsageSessionId;

class WellbeingEntityMapper {

    // -------------------------------------------------------------------------
    // FocusSession
    // -------------------------------------------------------------------------

    FocusSession toDomain(FocusSessionJpaEntity e) {
        InterruptionReason reason = e.getInterruptionReason() != null
            ? InterruptionReason.valueOf(e.getInterruptionReason())
            : null;
        return FocusSession.reconstitute(
            FocusSessionId.of(e.getId()),
            UserId.of(e.getUserId()),
            e.getStartedAt(),
            FocusSessionStatus.valueOf(e.getStatus()),
            e.getEndedAt(),
            reason
        );
    }

    FocusSessionJpaEntity toJpaEntity(FocusSession s) {
        return FocusSessionJpaEntity.of(
            s.id().value(),
            s.userId().value(),
            s.startedAt(),
            s.status().name(),
            s.endedAt().orElse(null),
            s.interruptionReason().map(Enum::name).orElse(null)
        );
    }

    // -------------------------------------------------------------------------
    // UsageSession
    // -------------------------------------------------------------------------

    UsageSession toDomain(UsageSessionJpaEntity e) {
        return UsageSession.reconstitute(
            UsageSessionId.of(e.getId()),
            UserId.of(e.getUserId()),
            e.getAppName(),
            AppType.valueOf(e.getAppType()),
            e.getStartedAt(),
            e.getEndedAt()
        );
    }

    UsageSessionJpaEntity toJpaEntity(UsageSession s) {
        return UsageSessionJpaEntity.of(
            s.id().value(),
            s.userId().value(),
            s.appName(),
            s.appType().name(),
            s.startedAt(),
            s.endedAt().orElse(null)
        );
    }
}
