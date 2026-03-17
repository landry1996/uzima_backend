package com.uzima.infrastructure.persistence.wellbeing;

import com.uzima.application.wellbeing.port.out.UsageSessionRepositoryPort;
import com.uzima.domain.user.model.UserId;
import com.uzima.domain.wellbeing.model.AppType;
import com.uzima.domain.wellbeing.model.UsageSession;
import com.uzima.domain.wellbeing.model.UsageSessionId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class UsageSessionRepositoryAdapter implements UsageSessionRepositoryPort {

    private final SpringDataUsageSessionRepository jpa;
    private final WellbeingEntityMapper            mapper = new WellbeingEntityMapper();

    public UsageSessionRepositoryAdapter(SpringDataUsageSessionRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(UsageSession session) {
        jpa.save(mapper.toJpaEntity(session));
    }

    @Override
    public Optional<UsageSession> findById(UsageSessionId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<UsageSession> findByUserId(UserId userId) {
        return jpa.findByUserId(userId.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<UsageSession> findByUserIdAndAppType(UserId userId, AppType appType) {
        return jpa.findByUserIdAndAppType(userId.value(), appType.name())
                  .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<UsageSession> findByUserIdBetween(UserId userId, Instant from, Instant to) {
        return jpa.findByUserIdAndStartedAtBetween(userId.value(), from, to)
                  .stream().map(mapper::toDomain).toList();
    }
}
