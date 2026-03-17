package com.uzima.domain.shared.exception;

import com.uzima.domain.shared.DomainException;

/**
 * Exception levée lors de la violation d'une règle métier complexe.
 * Différence avec les exceptions de Value Objects (InvalidPhoneNumberException, etc.) :
 * - Les VO exceptions indiquent un format invalide (problème de données)
 * - BusinessRuleViolationException indique une règle de gestion violée
 *   (ex : "un QR code révoqué ne peut pas être scanné", "l'expéditeur n'est pas participant")
 * Cette exception est au niveau du domaine : pas de dépendance technique.
 */
public class BusinessRuleViolationException extends DomainException {

    private final String ruleCode;

    public BusinessRuleViolationException(String ruleCode, String message) {
        super(message);
        this.ruleCode = ruleCode;
    }

    /**
     * Code de la règle métier violée (utilisé pour le logging structuré et la traduction HTTP).
     * Ex : "QR_CODE_REVOKED", "SENDER_NOT_IN_CONVERSATION"
     */
    public String ruleCode() {
        return ruleCode;
    }
}
