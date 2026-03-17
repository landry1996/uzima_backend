package com.uzima.application.shared.exception;

/**
 * Exception racine de la couche application.
 * Séparée de DomainException pour permettre la distinction sémantique :
 * - DomainException → violation d'invariant ou règle métier (domaine pur)
 * - ApplicationException → problème d'orchestration, accès, ressource non trouvée
 * Ces exceptions NE DÉPENDENT PAS de l'infrastructure.
 * Elles sont traduites en réponses HTTP par le GlobalExceptionHandler (bootstrap).
 */
public abstract class ApplicationException extends RuntimeException {

    private final String errorCode;

    protected ApplicationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Code d'erreur lisible par machine (utilisé par le client front-end).
     * Ex : "USER_NOT_FOUND", "UNAUTHORIZED_ACCESS"
     */
    public String errorCode() {
        return errorCode;
    }
}