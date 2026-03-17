package com.uzima.infrastructure.shared.exception;

/**
 * Défaillance de la couche de persistance (JPA, JDBC, PostgreSQL).
 * <p>
 * Encapsule les exceptions Spring Data / Hibernate pour éviter
 * leur propagation vers les couches supérieures.
 * Traduite en HTTP 503 Service Unavailable par le GlobalExceptionHandler.
 */
public class DatabaseException extends InfrastructureException {

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
