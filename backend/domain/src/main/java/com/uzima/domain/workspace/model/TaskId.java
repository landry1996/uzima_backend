package com.uzima.domain.workspace.model;

import java.util.Objects;
import java.util.UUID;

/** Value Object : Identifiant unique d'une tâche. */
public record TaskId(UUID value) {

    public TaskId {
        Objects.requireNonNull(value, "L'identifiant de tâche ne peut pas être nul");
    }

    public static TaskId generate() { return new TaskId(UUID.randomUUID()); }
    public static TaskId of(UUID uuid) { return new TaskId(uuid); }
    public static TaskId of(String uuid) {
        try { return new TaskId(UUID.fromString(uuid)); }
        catch (IllegalArgumentException e) { throw new InvalidTaskIdException("Format UUID invalide : " + uuid); }
    }

    @Override public String toString() { return value.toString(); }

    public static final class InvalidTaskIdException extends RuntimeException {
        public InvalidTaskIdException(String message) { super(message); }
    }
}
