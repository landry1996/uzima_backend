package com.uzima.infrastructure.security;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.uzima.application.user.port.out.PhoneValidationPort;

/**
 * Adaptateur : Validation de numéro de téléphone via libphonenumber (Google).
 * Implémente PhoneValidationPort défini dans la couche application.
 * Confiné à l'infrastructure — aucune logique métier.
 */
public final class LibPhoneNumberAdapter implements PhoneValidationPort {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();

    @Override
    public ValidationResult validate(String rawPhoneNumber, String countryCode) {
        try {
            Phonenumber.PhoneNumber parsed = PHONE_UTIL.parse(rawPhoneNumber, countryCode.toUpperCase());

            if (!PHONE_UTIL.isValidNumberForRegion(parsed, countryCode.toUpperCase())) {
                throw new PhoneValidationException(
                    "Le numéro '" + rawPhoneNumber + "' n'est pas valide pour le pays '" + countryCode + "'"
                );
            }

            String normalized = PHONE_UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
            return ValidationResult.valid(normalized, countryCode.toUpperCase());

        } catch (NumberParseException e) {
            throw new PhoneValidationException(
                "Impossible d'analyser le numéro '" + rawPhoneNumber + "' : " + e.getMessage()
            );
        }
    }
}
