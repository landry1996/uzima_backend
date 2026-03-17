package com.uzima.infrastructure.persistence.user;

import com.uzima.domain.user.model.*;

/**
 * Mapper : Conversion bidirectionnelle entre User (domaine) et UserJpaEntity (infrastructure).
 * <p>
 * Justification du pattern Mapper :
 * - Séparation des responsabilités : l'adaptateur coordonne, le mapper transforme
 * - Testabilité : le mapping peut être testé indépendamment sans Spring ni DB
 * - Évolutivité : si la structure JPA change, seul ce mapper est modifié
 * <p>
 * RÈGLES :
 * - Pas de logique métier ici (pas de validation, pas de règle de gestion)
 * - Correspondance structurelle uniquement
 */
public final class UserEntityMapper {

    private UserEntityMapper() {}

    /**
     * Domaine → JPA : prépare la persistance.
     */
    public static UserJpaEntity toJpaEntity(User user) {
        return UserJpaEntity.of(
                user.id().value(),
                user.phoneNumber().value(),
                user.country().value(),
                user.firstName().value(),
                user.lastName().value(),
                user.avatarUrl().orElse(null),
                user.presenceStatus(),
                user.isPremium(),
                user.passwordHash(),
                user.createdAt()
        );
    }

    /**
     * JPA → Domaine : reconstitue l'agrégat depuis la persistance.
     * <p>
     * Utilise User.reconstitute() (factory method de reconstitution)
     * et non User.register() (factory de création) pour éviter les
     * effets de bord (pas d'événement UserRegistered émis ici).
     */
    public static User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                UserId.of(entity.getId()),
                PhoneNumber.of(entity.getPhoneNumber()),
                CountryCode.of(entity.getCountryCode()),
                FirstName.of(entity.getFirstName()),
                LastName.of(entity.getLastName()),
                entity.getPasswordHash(),
                entity.getCreatedAt(),
                entity.getPresenceStatus(),
                entity.isPremium(),
                entity.getAvatarUrl()
        );
    }
}
