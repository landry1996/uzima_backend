package com.uzima.security.token.port;

import com.uzima.security.token.model.TokenClaims;

/**
 * Port de sortie : Vérification et extraction des claims d'un access token JWT.
 * Implémenté dans le module infrastructure.
 * Sépare la vérification de la génération pour respecter le principe de responsabilité unique.
 */
public interface AccessTokenVerifierPort {

    /**
     * Vérifie la signature, l'expiration et l'intégrité du token JWT.
     *
     * @param rawToken La valeur brute du JWT (sans le préfixe "Bearer ")
     * @return Les claims extraits et vérifiés
     * @throws InvalidTokenException si le token est invalide, expiré ou falsifié
     */
    TokenClaims verify(String rawToken);

    final class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }

        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
