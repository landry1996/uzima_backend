package com.uzima.application.assistant.port.out;

import com.uzima.domain.assistant.model.Reminder;
import com.uzima.domain.assistant.model.ReminderId;
import com.uzima.domain.assistant.model.ReminderStatus;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Optional;

/** Port OUT (application) — délègue au port domaine ReminderRepository. */
public interface ReminderRepositoryPort {

    void save(Reminder reminder);

    Optional<Reminder> findById(ReminderId id);

    List<Reminder> findByUserId(UserId userId);

    List<Reminder> findByUserIdAndStatus(UserId userId, ReminderStatus status);

    List<Reminder> findActiveByUserId(UserId userId);
}
