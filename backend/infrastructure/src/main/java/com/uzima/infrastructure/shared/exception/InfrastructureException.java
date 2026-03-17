package com.uzima.infrastructure.shared.exception;

/**
 * Exception racine de la couche infrastructure.
 * <p>
 * Représente les défaillances techniques (base de données, réseau, service externe).
 * Ne doit JAMAIS remonter jusqu'au domaine ou à l'application sans être traduite.
 * <p>
 * Les adaptateurs (ex: UserRepositoryAdapter) capturent les exceptions JPA/JDBC
 * et les encapsulent dans une sous-classe d'InfrastructureException.
 * Le GlobalExceptionHandler les traduit en réponse HTTP 5xx.
 * <p>
 * Pas de fuite d'information : les messages techniques ne sont pas exposés au client.
 */
public abstract class InfrastructureException extends RuntimeException {

    protected InfrastructureException(String message) {
        super(message);
    }

    protected InfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
