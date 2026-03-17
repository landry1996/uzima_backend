package com.uzima.infrastructure.persistence.social;

import com.uzima.domain.social.model.CircleType;
import com.uzima.domain.social.model.NotificationPolicy;
import com.uzima.domain.social.model.VisibilityLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité JPA : Table 'circles'.
 * Infrastructure uniquement. Pas de logique métier.
 * <p>
 * Les champs de CircleRule (value object) sont stockés à plat dans la table
 * (pattern Embedded Value / Value Object flattening).
 * <p>
 * Les memberships sont gérés en cascade (aggregate boundary).
 */
@Entity
@Table(
    name = "circles",
    indexes = {
        @Index(name = "idx_circles_owner_id",    columnList = "owner_id"),
        @Index(name = "idx_circles_type",        columnList = "type"),
        @Index(name = "idx_circles_created_at",  columnList = "created_at DESC")
    }
)
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CircleJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CircleType type;

    @Column(name = "owner_id", nullable = false, columnDefinition = "uuid")
    private UUID ownerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // -------------------------------------------------------------------------
    // CircleRule (value object embarqué — colonnes plates)
    // -------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_policy", nullable = false, length = 20)
    private NotificationPolicy notificationPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 15)
    private VisibilityLevel visibility;

    @Column(name = "allows_voice_messages", nullable = false)
    private boolean allowsVoiceMessages;

    @Column(name = "allows_payments", nullable = false)
    private boolean allowsPayments;

    // -------------------------------------------------------------------------
    // Memberships (aggregate boundary — cascade ALL)
    // -------------------------------------------------------------------------

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "circle_id", nullable = false)
    private List<CircleMembershipJpaEntity> memberships = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Factory statique (pas de constructeur public)
    // -------------------------------------------------------------------------

    public static CircleJpaEntity of(
            UUID            id,
            String          name,
            CircleType      type,
            UUID            ownerId,
            NotificationPolicy notificationPolicy,
            VisibilityLevel visibility,
            boolean         allowsVoiceMessages,
            boolean         allowsPayments,
            Instant         createdAt,
            List<CircleMembershipJpaEntity> memberships
    ) {
        CircleJpaEntity e = new CircleJpaEntity();
        e.id                  = id;
        e.name                = name;
        e.type                = type;
        e.ownerId             = ownerId;
        e.notificationPolicy  = notificationPolicy;
        e.visibility          = visibility;
        e.allowsVoiceMessages = allowsVoiceMessages;
        e.allowsPayments      = allowsPayments;
        e.createdAt           = createdAt;
        e.memberships.addAll(memberships);
        return e;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CircleJpaEntity c)) return false;
        return id != null && id.equals(c.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
