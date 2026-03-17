package com.uzima.infrastructure.persistence.social;

import com.uzima.domain.social.model.Circle;
import com.uzima.domain.social.model.CircleId;
import com.uzima.domain.social.model.CircleMembership;
import com.uzima.domain.social.model.CircleRule;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.UUID;

/**
 * Mapper : Conversion bidirectionnelle entre Circle (domaine) et CircleJpaEntity (infra).
 * <p>
 * Utilise Circle.reconstitute() pour l'hydratation — jamais de constructeur public.
 * Les memberships sont mappés via CircleMembership (constructeur package-private depuis le domaine).
 */
public final class CircleEntityMapper {

    private CircleEntityMapper() {}

    // -------------------------------------------------------------------------
    // Domaine → JPA
    // -------------------------------------------------------------------------

    public static CircleJpaEntity toJpaEntity(Circle circle) {
        List<CircleMembershipJpaEntity> membershipEntities = circle.memberships().stream()
                .map(m -> CircleMembershipJpaEntity.of(
                    UUID.randomUUID(),         // id technique auto-généré à chaque save
                    circle.id().value(),
                    m.memberId().value(),
                    m.role(),
                    m.joinedAt()
                ))
                .toList();

        return CircleJpaEntity.of(
            circle.id().value(),
            circle.name(),
            circle.type(),
            circle.ownerId().value(),
            circle.rules().notificationPolicy(),
            circle.rules().visibility(),
            circle.rules().allowsVoiceMessages(),
            circle.rules().allowsPayments(),
            circle.createdAt(),
            membershipEntities
        );
    }

    // -------------------------------------------------------------------------
    // JPA → Domaine
    // -------------------------------------------------------------------------

    public static Circle toDomain(CircleJpaEntity entity) {
        CircleRule rules = new CircleRule(
            entity.getNotificationPolicy(),
            entity.getVisibility(),
            entity.isAllowsVoiceMessages(),
            entity.isAllowsPayments()
        );

        List<CircleMembership> memberships = entity.getMemberships().stream()
                .map(m -> CircleMembership.reconstitute(
                    UserId.of(m.getMemberId()),
                    m.getRole(),
                    m.getJoinedAt()
                ))
                .toList();

        return Circle.reconstitute(
            CircleId.of(entity.getId()),
            entity.getName(),
            entity.getType(),
            UserId.of(entity.getOwnerId()),
            rules,
            entity.getCreatedAt(),
            memberships
        );
    }
}
