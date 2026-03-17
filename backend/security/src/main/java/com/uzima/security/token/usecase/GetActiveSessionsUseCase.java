package com.uzima.security.token.usecase;

import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import com.uzima.security.token.model.RefreshToken;
import com.uzima.security.token.port.RefreshTokenRepositoryPort;

import java.util.List;
import java.util.Objects;

/**
 * Use Case : Lister les sessions actives d'un utilisateur.
 * <p>
 * Chaque session active correspond à un refresh token non révoqué et non expiré.
 * Utile pour afficher "où vous êtes connecté" et permettre la révocation ciblée.
 * <p>
 * Une vérification défensive via {@code isUsable()} est appliquée en plus du filtre repository,
 * pour garantir la cohérence même si la requête JPA retourne des résultats légèrement décalés.
 */
public final class GetActiveSessionsUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final TimeProvider clock;

    public GetActiveSessionsUseCase(RefreshTokenRepositoryPort refreshTokenRepository, TimeProvider clock) {
        this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository, "Le repository est obligatoire");
        this.clock                  = Objects.requireNonNull(clock,                  "Le fournisseur de temps est obligatoire");
    }

    /**
     * @param userId L'identifiant de l'utilisateur
     * @return La liste des refresh tokens actifs (non révoqués, non expirés)
     */
    public List<RefreshToken> execute(UserId userId) {
        Objects.requireNonNull(userId, "userId est obligatoire");
        return refreshTokenRepository.findActiveForUser(userId)
                .stream()
                .filter(token -> token.isUsable(clock.now()))
                .toList();
    }
}
