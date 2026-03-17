package com.uzima.domain.payment.port;

import com.uzima.domain.payment.model.Wallet;
import com.uzima.domain.user.model.UserId;

import java.util.Optional;

/**
 * Port OUT (sortie) : Persistance des portefeuilles internes Uzima.
 * Implémenté dans l'infrastructure (WalletRepositoryAdapter).
 */
public interface WalletRepositoryPort {

    /**
     * Recherche le portefeuille d'un utilisateur.
     *
     * @param userId Identifiant de l'utilisateur propriétaire
     * @return Le portefeuille si trouvé, {@link Optional#empty()} sinon
     */
    Optional<Wallet> findByOwnerId(UserId userId);

    /**
     * Persiste ou met à jour un portefeuille.
     *
     * @param wallet Le portefeuille à sauvegarder
     */
    void save(Wallet wallet);
}
