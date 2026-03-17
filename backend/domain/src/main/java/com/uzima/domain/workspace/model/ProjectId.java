package com.uzima.domain.workspace.model;

import java.util.Objects;
import java.util.UUID;

/** Value Object : Identifiant unique d'un projet. */
public record ProjectId(UUID value) {

    public ProjectId {
        Objects.requireNonNull(value, "L'identifiant de projet ne peut pas être nul");
    }

    public static ProjectId generate() { return new ProjectId(UUID.randomUUID()); }
    public static ProjectId of(UUID uuid) { return new ProjectId(uuid); }
    public static ProjectId of(String uuid) {
        try { return new ProjectId(UUID.fromString(uuid)); }
        catch (IllegalArgumentException e) { throw new InvalidProjectIdException("Format UUID invalide : " + uuid); }
    }

    @Override public String toString() { return value.toString(); }

    public static final class InvalidProjectIdException extends RuntimeException {
        public InvalidProjectIdException(String message) { super(message); }
    }
}
