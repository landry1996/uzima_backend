package com.uzima.application.assistant;

import com.uzima.application.assistant.port.out.ReminderRepositoryPort;
import com.uzima.domain.assistant.model.Reminder;
import com.uzima.domain.assistant.model.ReminderStatus;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Objects;

/** Use Case : Récupérer les rappels d'un utilisateur. */
public class GetRemindersUseCase {

    private final ReminderRepositoryPort repository;

    public GetRemindersUseCase(ReminderRepositoryPort repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public List<Reminder> findAll(UserId userId) {
        Objects.requireNonNull(userId, "userId est obligatoire");
        return repository.findByUserId(userId);
    }

    public List<Reminder> findActive(UserId userId) {
        Objects.requireNonNull(userId, "userId est obligatoire");
        return repository.findActiveByUserId(userId);
    }

    public List<Reminder> findByStatus(UserId userId, ReminderStatus status) {
        Objects.requireNonNull(userId, "userId est obligatoire");
        Objects.requireNonNull(status, "status est obligatoire");
        return repository.findByUserIdAndStatus(userId, status);
    }
}
