package com.uzima.application.shared.exception;

/**
 * Exception applicative : Conflit de données.
 * Traduite en HTTP 409 par le GlobalExceptionHandler.
 * <p>
 * Utilisée lorsqu'une ressource existe déjà ou qu'il y a un état incompatible.
 * <p>
 * Utiliser UNIQUEMENT les factory methods pour créer des instances.
 */
public final class ConflictException extends ApplicationException {

    private ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }

    public static ConflictException focusSessionAlreadyActive(Object sessionId) {
        return new ConflictException(
                "FOCUS_SESSION_ALREADY_ACTIVE",
                "Une session de focus est déjà en cours : " + sessionId
        );
    }
}
