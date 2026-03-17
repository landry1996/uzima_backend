package com.uzima.security.token.port;

/**
 * Port de sortie : Hachage sécurisé des refresh tokens.
 * Les refresh tokens sont des valeurs aléatoires opaques (pas des JWT).
 * Ils sont stockés en base sous forme hachée (SHA-256 ou Argon2) pour éviter
 * qu'un accès non autorisé à la base permette de les réutiliser.
 * Implémenté dans infrastructure.
 */
public interface RefreshTokenHasherPort {

    /**
     * Hache la valeur brute d'un refresh token.
     *
     * @param rawValue Valeur brute générée aléatoirement
     * @return Hash sécurisé à stocker en base
     */
    String hash(String rawValue);

    /**
     * Génère une valeur aléatoire sécurisée pour un nouveau refresh token.
     * Doit utiliser SecureRandom ou équivalent.
     *
     * @return Valeur brute opaque (64 caractères hexadécimaux par exemple)
     */
    String generateRawValue();
}
