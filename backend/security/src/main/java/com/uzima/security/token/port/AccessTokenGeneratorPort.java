package com.uzima.security.token.port;

import com.uzima.domain.user.model.UserId;
import com.uzima.security.token.model.AccessToken;

/**
 * Port de sortie : Génération des access tokens JWT.
 * Implémenté dans le module infrastructure (JwtAccessTokenGenerator).
 * Le module security définit le contrat, l'infrastructure livre l'implémentation.
 */
public interface AccessTokenGeneratorPort {

    /**
     * Génère un access token JWT signé pour l'utilisateur donné.
     * La durée de vie est déterminée par la configuration de l'implémentation.
     *
     * @param userId Identifiant de l'utilisateur authentifié
     * @return Access token avec sa valeur JWT brute et ses métadonnées
     */
    AccessToken generate(UserId userId);
}
