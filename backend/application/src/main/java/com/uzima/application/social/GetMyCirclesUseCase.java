package com.uzima.application.social;

import com.uzima.application.social.port.out.CircleRepositoryPort;
import com.uzima.domain.social.model.Circle;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Objects;

/**
 * Use Case (Query) : Récupération de tous les cercles d'un utilisateur.
 * <p>
 * Retourne une vue structurée distinguant :
 * - les cercles possédés (OWNER)
 * - les cercles rejoints (MEMBER / ADMIN / GUEST)
 */
public final class GetMyCirclesUseCase {

    private final CircleRepositoryPort circleRepository;

    public GetMyCirclesUseCase(CircleRepositoryPort circleRepository) {
        this.circleRepository = Objects.requireNonNull(circleRepository, "Le repository de cercles est obligatoire");
    }

    /**
     * @param userId Identifiant de l'utilisateur
     * @return Vue structurée des cercles (owned + joined)
     */
    public MyCirclesView execute(UserId userId) {
        Objects.requireNonNull(userId, "L'identifiant utilisateur est obligatoire");

        List<Circle> owned  = circleRepository.findByOwnerId(userId);
        List<Circle> joined = circleRepository.findByMemberId(userId).stream()
                .filter(c -> !c.isOwner(userId))   // exclure les cercles déjà dans owned
                .toList();

        return new MyCirclesView(owned, joined);
    }

    // -------------------------------------------------------------------------
    // Vue de sortie
    // -------------------------------------------------------------------------

    /**
     * Vue agrégée des cercles d'un utilisateur.
     *
     * @param owned  Cercles dont l'utilisateur est OWNER
     * @param joined Cercles dont l'utilisateur est MEMBER / ADMIN / GUEST (mais pas OWNER)
     */
    public record MyCirclesView(
            List<Circle> owned,
            List<Circle> joined
    ) {}
}
