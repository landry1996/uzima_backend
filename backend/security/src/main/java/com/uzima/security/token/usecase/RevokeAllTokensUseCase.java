package com.uzima.security.token.usecase;

import com.uzima.domain.user.model.UserId;
import com.uzima.security.token.port.RefreshTokenRepositoryPort;

import java.util.Objects;

/**
 * Use Case : Révocation de tous les tokens d'un utilisateur.
 * Utilisé pour :
 * - Déconnexion globale (logout all devices)
 * - Réponse à une compromission de compte
 * - Changement de mot de passe
 * Pas de Spring. Pas de framework.
 */
public final class RevokeAllTokensUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepository;

    public RevokeAllTokensUseCase(RefreshTokenRepositoryPort refreshTokenRepository) {
        this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository);
    }

    /**
     * Révoque tous les refresh tokens actifs de l'utilisateur.
     * Les access tokens existants resteront valides jusqu'à leur expiration naturelle
     * (ils sont stateless). C'est un compromis acceptable vu leur courte durée de vie.
     *
     * @param userId Identifiant de l'utilisateur
     */
    public void execute(UserId userId) {
        Objects.requireNonNull(userId, "Le userId est obligatoire");
        refreshTokenRepository.revokeAllForUser(userId);
    }
}
