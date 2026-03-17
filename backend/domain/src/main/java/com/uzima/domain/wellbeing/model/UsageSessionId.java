package com.uzima.domain.wellbeing.model;

import java.util.Objects;
import java.util.UUID;

/** Value Object : Identifiant unique d'une session d'utilisation. */
public record UsageSessionId(UUID value) {

    public UsageSessionId {
        Objects.requireNonNull(value, "L'identifiant de session d'utilisation ne peut pas être nul");
    }

    public static UsageSessionId generate() { return new UsageSessionId(UUID.randomUUID()); }
    public static UsageSessionId of(UUID uuid) { return new UsageSessionId(uuid); }
    public static UsageSessionId of(String uuid) {
        try { return new UsageSessionId(UUID.fromString(uuid)); }
        catch (IllegalArgumentException e) {
            throw new InvalidUsageSessionIdException("Format UUID invalide : " + uuid);
        }
    }

    @Override public String toString() { return value.toString(); }

    public static final class InvalidUsageSessionIdException extends RuntimeException {
        public InvalidUsageSessionIdException(String message) { super(message); }
    }
}
