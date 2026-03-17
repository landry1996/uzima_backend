package com.uzima.infrastructure.persistence.social;

import com.uzima.domain.social.model.MemberRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité JPA : Table 'circle_memberships'.
 * Infrastructure uniquement. Pas de logique métier.
 * <p>
 * Appartient au cycle de vie de CircleJpaEntity (cascade ALL, orphanRemoval).
 */
@Entity
@Table(
    name = "circle_memberships",
    indexes = {
        @Index(name = "idx_memberships_circle_id", columnList = "circle_id"),
        @Index(name = "idx_memberships_member_id", columnList = "member_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_memberships_circle_member", columnNames = {"circle_id", "member_id"})
    }
)
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CircleMembershipJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "circle_id", nullable = false, columnDefinition = "uuid")
    private UUID circleId;

    @Column(name = "member_id", nullable = false, columnDefinition = "uuid")
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private MemberRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    public static CircleMembershipJpaEntity of(UUID id, UUID circleId, UUID memberId, MemberRole role, Instant joinedAt) {
        CircleMembershipJpaEntity e = new CircleMembershipJpaEntity();
        e.id       = id;
        e.circleId = circleId;
        e.memberId = memberId;
        e.role     = role;
        e.joinedAt = joinedAt;
        return e;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CircleMembershipJpaEntity m)) return false;
        return id != null && id.equals(m.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
