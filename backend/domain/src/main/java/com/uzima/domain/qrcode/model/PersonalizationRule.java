package com.uzima.domain.qrcode.model;

import java.util.Objects;

/**
 * Value Object : Règle de personnalisation contextuelle d'un QR Code.
 * Permet de conditionner le comportement du QR Code selon le profil
 * de l'utilisateur ou un contexte particulier.
 * Exemples :
 * - condition = "WORK_HOURS", targetProfile = "COLLEAGUE"
 * - condition = "EVENING", targetProfile = "FRIEND"
 * - condition = "ALWAYS", targetProfile = "ANYONE"
 */
public record PersonalizationRule(
        String condition,
        String targetProfile
) {

    public PersonalizationRule {
        Objects.requireNonNull(condition,     "La condition est obligatoire");
        Objects.requireNonNull(targetProfile, "Le profil cible est obligatoire");
        if (condition.isBlank())     throw new InvalidPersonalizationRuleException("La condition ne peut pas être vide");
        if (targetProfile.isBlank()) throw new InvalidPersonalizationRuleException("Le profil cible ne peut pas être vide");
    }

    public static PersonalizationRule of(String condition, String targetProfile) {
        return new PersonalizationRule(condition, targetProfile);
    }

    /** Règle par défaut : toujours actif pour tout le monde. */
    public static PersonalizationRule alwaysForAnyone() {
        return new PersonalizationRule("ALWAYS", "ANYONE");
    }

    public static final class InvalidPersonalizationRuleException extends RuntimeException {
        public InvalidPersonalizationRuleException(String message) { super(message); }
    }
}
