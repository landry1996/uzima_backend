package com.uzima.domain.shared;

import java.time.Instant;
import java.util.UUID;

/**
 * Interface de base pour les événements du domaine.
 * Utilisation d'une interface (et non d'une classe abstraite) pour permettre
 * aux Java Records d'implémenter ce contrat (les records ne peuvent pas
 * étendre de classes autres que java.lang.Record).
 * Les événements de domaine représentent quelque chose qui s'est produit.
 * Ils sont immuables et horodatés.
 */
public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();
}
