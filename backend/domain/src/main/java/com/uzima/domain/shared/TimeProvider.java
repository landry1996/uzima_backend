package com.uzima.domain.shared;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Port d'abstraction du temps.
 * Le domaine NE DOIT JAMAIS appeler Instant.now() ou LocalDate.now() directement.
 * Cette interface est implémentée par l'infrastructure (SystemTimeProvider)
 * et peut être remplacée par un stub déterministe en tests.
 * Principe : dépendance au temps injectée, pas appelée.
 */
public interface TimeProvider {

    Instant now();

    default LocalDate today() {
        return LocalDate.ofInstant(now(), java.time.ZoneOffset.UTC);
    }
}
