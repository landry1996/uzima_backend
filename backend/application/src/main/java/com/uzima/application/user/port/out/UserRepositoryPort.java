package com.uzima.application.user.port.out;

import com.uzima.domain.user.port.UserRepository;

/**
 * Port de sortie (couche application) : persistance des utilisateurs.
 * <p>
 * Étend le port du domaine sans en dupliquer le contrat.
 * Les use cases d'application dépendent de cette interface.
 * L'infrastructure implémente uniquement ce port (qui satisfait aussi UserRepository).
 * <p>
 * Méthodes héritées de UserRepository (suffisantes pour tous les use cases) :
 * - save(User)
 * - findById(UserId)
 * - findByPhoneNumber(PhoneNumber)
 * - existsByPhoneNumber(PhoneNumber)
 */
public interface UserRepositoryPort extends UserRepository {}
