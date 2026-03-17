package com.uzima.domain.user.port;

import com.uzima.domain.user.model.PhoneNumber;
import com.uzima.domain.user.model.User;
import com.uzima.domain.user.model.UserId;

import java.util.Optional;

/**
 * Port de sortie : Accès à la persistance des utilisateurs.
 * <p>
 * Cette interface appartient au DOMAINE.
 * Elle définit le contrat que toute implémentation (JPA, in-memory, MongoDB)
 * doit respecter.
 * <p>
 * Positionnement dans l'architecture :
 * - Les services de DOMAINE (ex: AccountLockoutPolicy étendu) utilisent ce port
 * - Les use cases d'APPLICATION utilisent UserRepositoryPort (application/user/port/out/)
 *   qui ÉTEND cette interface — évitant toute duplication de contrat
 * - L'INFRASTRUCTURE implémente UserRepositoryPort (qui étend ce port)
 * <p>
 * Principe DIP :
 * - Le domaine définit l'interface
 * - L'infrastructure la réalise
 * - Le domaine ne dépend jamais de l'infrastructure
 */
public interface UserRepository {

    /** Persiste un utilisateur (création ou mise à jour). */
    void save(User user);

    /** Recherche par identifiant technique. */
    Optional<User> findById(UserId id);

    /**
     * Recherche par numéro de téléphone (E.164 normalisé).
     * Utilisé pour l'authentification et la vérification d'unicité.
     */
    Optional<User> findByPhoneNumber(PhoneNumber phoneNumber);

    /**
     * Vérifie l'existence d'un numéro de téléphone (plus léger qu'un findBy).
     * Utilisé lors de l'inscription pour éviter les doublons.
     */
    boolean existsByPhoneNumber(PhoneNumber phoneNumber);
}
