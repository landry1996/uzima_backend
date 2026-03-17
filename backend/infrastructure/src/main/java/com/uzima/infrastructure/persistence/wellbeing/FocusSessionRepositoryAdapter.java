package com.uzima.infrastructure.persistence.wellbeing;

import com.uzima.application.wellbeing.port.out.FocusSessionRepositoryPort;
import com.uzima.domain.user.model.UserId;
import com.uzima.domain.wellbeing.model.FocusSession;
import com.uzima.domain.wellbeing.model.FocusSessionId;
import com.uzima.domain.wellbeing.model.FocusSessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class FocusSessionRepositoryAdapter implements FocusSessionRepositoryPort {

    private final SpringDataFocusSessionRepository jpa;
    private final WellbeingEntityMapper            mapper = new WellbeingEntityMapper();

    public FocusSessionRepositoryAdapter(SpringDataFocusSessionRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(FocusSession session) {
        jpa.save(mapper.toJpaEntity(session));
    }

    @Override
    public Optional<FocusSession> findById(FocusSessionId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<FocusSession> findByUserId(UserId userId) {
        return jpa.findByUserId(userId.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<FocusSession> findByUserIdAndStatus(UserId userId, FocusSessionStatus status) {
        return jpa.findByUserIdAndStatus(userId.value(), status.name())
                  .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<FocusSession> findByUserIdBetween(UserId userId, Instant from, Instant to) {
        return jpa.findByUserIdAndStartedAtBetween(userId.value(), from, to)
                  .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<FocusSession> findActiveByUserId(UserId userId) {
        return jpa.findFirstByUserIdAndStatus(userId.value(), FocusSessionStatus.ACTIVE.name())
                  .map(mapper::toDomain);
    }
}
