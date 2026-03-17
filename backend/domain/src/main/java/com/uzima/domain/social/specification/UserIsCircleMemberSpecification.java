package com.uzima.domain.social.specification;

import com.uzima.domain.shared.specification.Specification;
import com.uzima.domain.social.model.Circle;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Specification : Vérifie qu'un utilisateur est membre d'un cercle
 * (quel que soit son rôle : OWNER, ADMIN, MEMBER, GUEST).
 * <p>
 * Utilisée pour contrôler l'accès aux contenus d'un cercle.
 */
public final class UserIsCircleMemberSpecification implements Specification<Circle> {

    private final UserId userId;

    public UserIsCircleMemberSpecification(UserId userId) {
        this.userId = Objects.requireNonNull(userId, "L'identifiant utilisateur est obligatoire");
    }

    @Override
    public boolean isSatisfiedBy(Circle circle) {
        return circle.isMember(userId);
    }
}
