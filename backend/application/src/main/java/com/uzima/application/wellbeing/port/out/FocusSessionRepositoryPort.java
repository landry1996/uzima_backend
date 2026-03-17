package com.uzima.application.wellbeing.port.out;

import com.uzima.domain.user.model.UserId;
import com.uzima.domain.wellbeing.model.FocusSession;
import com.uzima.domain.wellbeing.model.FocusSessionId;
import com.uzima.domain.wellbeing.model.FocusSessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Port OUT (application) — sessions de focus. */
public interface FocusSessionRepositoryPort {

    void save(FocusSession session);

    Optional<FocusSession> findById(FocusSessionId id);

    List<FocusSession> findByUserId(UserId userId);

    List<FocusSession> findByUserIdAndStatus(UserId userId, FocusSessionStatus status);

    List<FocusSession> findByUserIdBetween(UserId userId, Instant from, Instant to);

    Optional<FocusSession> findActiveByUserId(UserId userId);
}
