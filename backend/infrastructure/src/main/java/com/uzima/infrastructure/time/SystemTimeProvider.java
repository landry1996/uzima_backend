package com.uzima.infrastructure.time;

import com.uzima.domain.shared.TimeProvider;

import java.time.Instant;

/**
 * Adaptateur : Implémentation du TimeProvider basée sur l'horloge système.
 * <p>
 * C'est la SEULE classe du projet autorisée à appeler Instant.now().
 * Tout le reste du code utilise TimeProvider par injection.
 * <p>
 * Avantages :
 * - Les tests unitaires utilisent un stub déterministe → reproductibles
 * - La production utilise cette implémentation → comportement réel
 * - Aucune dépendance au temps système dans le domaine ou l'application
 */
public final class SystemTimeProvider implements TimeProvider {

    @Override
    public Instant now() {
        return Instant.now(); // Seul appel autorisé à Instant.now() dans tout le projet
    }
}
