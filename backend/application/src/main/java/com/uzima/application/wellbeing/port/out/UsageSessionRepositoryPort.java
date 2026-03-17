package com.uzima.application.wellbeing.port.out;

import com.uzima.domain.user.model.UserId;
import com.uzima.domain.wellbeing.model.AppType;
import com.uzima.domain.wellbeing.model.UsageSession;
import com.uzima.domain.wellbeing.model.UsageSessionId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Port OUT (application) — sessions d'utilisation d'apps. */
public interface UsageSessionRepositoryPort {

    void save(UsageSession session);

    Optional<UsageSession> findById(UsageSessionId id);

    List<UsageSession> findByUserId(UserId userId);

    List<UsageSession> findByUserIdBetween(UserId userId, Instant from, Instant to);

    List<UsageSession> findByUserIdAndAppType(UserId userId, AppType appType);
}
