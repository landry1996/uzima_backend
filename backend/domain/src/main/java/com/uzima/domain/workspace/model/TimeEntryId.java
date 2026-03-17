package com.uzima.domain.workspace.model;

import java.util.Objects;
import java.util.UUID;

/** Value Object : Identifiant unique d'une entrée de temps. */
public record TimeEntryId(UUID value) {

    public TimeEntryId {
        Objects.requireNonNull(value, "L'identifiant d'entrée de temps ne peut pas être nul");
    }

    public static TimeEntryId generate() { return new TimeEntryId(UUID.randomUUID()); }
    public static TimeEntryId of(UUID uuid) { return new TimeEntryId(uuid); }
    public static TimeEntryId of(String uuid) {
        try { return new TimeEntryId(UUID.fromString(uuid)); }
        catch (IllegalArgumentException e) { throw new InvalidTimeEntryIdException("Format UUID invalide : " + uuid); }
    }

    @Override public String toString() { return value.toString(); }

    public static final class InvalidTimeEntryIdException extends RuntimeException {
        public InvalidTimeEntryIdException(String message) { super(message); }
    }
}
