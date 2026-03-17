package com.uzima.domain.assistant.model;

import java.util.Objects;
import java.util.UUID;

/** Value Object : Identifiant unique d'un rappel. */
public record ReminderId(UUID value) {

    public ReminderId {
        Objects.requireNonNull(value, "L'identifiant de rappel ne peut pas être nul");
    }

    public static ReminderId generate() { return new ReminderId(UUID.randomUUID()); }
    public static ReminderId of(UUID uuid) { return new ReminderId(uuid); }
    public static ReminderId of(String uuid) {
        try { return new ReminderId(UUID.fromString(uuid)); }
        catch (IllegalArgumentException e) { throw new InvalidReminderIdException("Format UUID invalide : " + uuid); }
    }

    @Override public String toString() { return value.toString(); }

    public static final class InvalidReminderIdException extends RuntimeException {
        public InvalidReminderIdException(String message) { super(message); }
    }
}
