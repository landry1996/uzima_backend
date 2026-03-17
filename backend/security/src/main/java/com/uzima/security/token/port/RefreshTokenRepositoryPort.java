package com.uzima.security.token.port;

import com.uzima.domain.user.model.UserId;
import com.uzima.security.token.model.RefreshToken;
import com.uzima.security.token.model.TokenFamily;
import com.uzima.security.token.model.TokenId;

import java.util.List;
import java.util.Optional;

/**
 * Port de sortie : Persistance des refresh tokens.
 * Implémenté dans le module infrastructure (JPA, Redis, etc.).
 * Les valeurs stockées sont HACHÉES (SHA-256) — jamais en clair.
 */
public interface RefreshTokenRepositoryPort {

    /** Sauvegarde un nouveau refresh token. */
    void save(RefreshToken refreshToken);

    /** Met à jour un refresh token existant (ex: après révocation). */
    void update(RefreshToken refreshToken);

    /**
     * Recherche par valeur hachée.
     * Utilisé lors de la rotation : le client envoie la valeur brute,
     * le service la hache et cherche en base.
     */
    Optional<RefreshToken> findByHashedValue(String hashedValue);

    /** Recherche par identifiant technique. */
    Optional<RefreshToken> findById(TokenId tokenId);

    /** Récupère tous les tokens actifs d'une famille (pour révocation en cascade). */
    List<RefreshToken> findByFamily(TokenFamily family);

    /** Révoque (marque comme révoqués) tous les tokens d'une famille. */
    void revokeFamily(TokenFamily family);

    /** Révoque tous les tokens actifs d'un utilisateur (logout global). */
    void revokeAllForUser(UserId userId);

    /** Supprime les tokens expirés (appelé par une tâche de maintenance). */
    void deleteExpired();

    /**
     * Récupère tous les refresh tokens actifs (non révoqués, non expirés) d'un utilisateur.
     * Utilisé lors du logout global pour lister les sessions ouvertes avant révocation.
     *
     * @param userId L'identifiant de l'utilisateur
     * @return La liste des tokens actifs, vide si aucun
     */
    List<RefreshToken> findActiveForUser(UserId userId);
}
