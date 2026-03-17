package com.uzima.infrastructure.shared.exception;

/**
 * Défaillance d'un service externe (notification push, SMS, paiement, etc.).
 * Traduite en HTTP 502 Bad Gateway par le GlobalExceptionHandler.
 */
public class ExternalServiceException extends InfrastructureException {

    private final String serviceName;

    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super("Service externe '" + serviceName + "' indisponible : " + message, cause);
        this.serviceName = serviceName;
    }

    public String serviceName() {
        return serviceName;
    }
}
