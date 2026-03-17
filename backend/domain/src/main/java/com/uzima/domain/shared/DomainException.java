package com.uzima.domain.shared;

/**
 * Exception racine du domaine.
 * Toutes les violations d'invariants métier doivent lever une sous-classe
 * de cette exception. Pas de RuntimeException générique dans le domaine.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
