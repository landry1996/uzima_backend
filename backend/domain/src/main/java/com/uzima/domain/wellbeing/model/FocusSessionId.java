package com.uzima.domain.wellbeing.model;

import java.util.Objects;
import java.util.UUID;

/** Value Object : Identifiant unique d'une session de focus. */
public record FocusSessionId(UUID value) {

    public FocusSessionId {
        Objects.requireNonNull(value, "L'identifiant de session de focus ne peut pas être nul");
    }

    public static FocusSessionId generate() { return new FocusSessionId(UUID.randomUUID()); }
    public static FocusSessionId of(UUID uuid) { return new FocusSessionId(uuid); }
    public static FocusSessionId of(String uuid) {
        try { return new FocusSessionId(UUID.fromString(uuid)); }
        catch (IllegalArgumentException e) {
            throw new InvalidFocusSessionIdException("Format UUID invalide : " + uuid);
        }
    }

    @Override public String toString() { return value.toString(); }

    public static final class InvalidFocusSessionIdException extends RuntimeException {
        public InvalidFocusSessionIdException(String message) { super(message); }
    }
}
