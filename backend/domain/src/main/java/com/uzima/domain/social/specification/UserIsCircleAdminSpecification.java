package com.uzima.domain.social.specification;

import com.uzima.domain.shared.specification.Specification;
import com.uzima.domain.social.model.Circle;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Specification : Vérifie qu'un utilisateur a les droits d'administration
 * dans un cercle (rôle ADMIN ou OWNER).
 * <p>
 * Utilisée pour protéger les opérations sensibles :
 * modification des règles, renommage, gestion des membres.
 */
public final class UserIsCircleAdminSpecification implements Specification<Circle> {

    private final UserId userId;

    public UserIsCircleAdminSpecification(UserId userId) {
        this.userId = Objects.requireNonNull(userId, "L'identifiant utilisateur est obligatoire");
    }

    @Override
    public boolean isSatisfiedBy(Circle circle) {
        return circle.isAdmin(userId);
    }
}
