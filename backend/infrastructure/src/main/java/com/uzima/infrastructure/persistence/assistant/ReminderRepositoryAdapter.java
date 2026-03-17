package com.uzima.infrastructure.persistence.assistant;

import com.uzima.application.assistant.port.out.ReminderRepositoryPort;
import com.uzima.domain.assistant.model.Reminder;
import com.uzima.domain.assistant.model.ReminderId;
import com.uzima.domain.assistant.model.ReminderStatus;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Optional;

public class ReminderRepositoryAdapter implements ReminderRepositoryPort {

    private final SpringDataReminderRepository jpa;
    private final ReminderEntityMapper         mapper = new ReminderEntityMapper();

    public ReminderRepositoryAdapter(SpringDataReminderRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Reminder reminder) {
        jpa.save(mapper.toJpaEntity(reminder));
    }

    @Override
    public Optional<Reminder> findById(ReminderId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Reminder> findByUserId(UserId userId) {
        return jpa.findByUserId(userId.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Reminder> findByUserIdAndStatus(UserId userId, ReminderStatus status) {
        return jpa.findByUserIdAndStatus(userId.value(), status.name())
                  .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Reminder> findActiveByUserId(UserId userId) {
        List<String> activeStatuses = List.of(
            ReminderStatus.PENDING.name(),
            ReminderStatus.SNOOZED.name()
        );
        return jpa.findByUserIdAndStatusIn(userId.value(), activeStatuses)
                  .stream().map(mapper::toDomain).toList();
    }
}
