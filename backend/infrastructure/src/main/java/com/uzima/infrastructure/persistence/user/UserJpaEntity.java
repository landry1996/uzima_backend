package com.uzima.infrastructure.persistence.user;

import com.uzima.domain.user.model.PresenceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité JPA : Table 'users'.
 * <p>
 * CETTE CLASSE N'EST PAS UN OBJET DU DOMAINE.
 * Elle sert uniquement à la persistance. Le mapping domaine <-> JPA
 * est effectué dans UserEntityMapper.
 * <p>
 * Lombok AUTORISÉ ici (infrastructure uniquement) : @Getter, @NoArgsConstructor.
 * @Builder et @Setter interdits.
 * @Data interdit (génère des setters et un equals/hashCode inadapté aux entités JPA).
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class UserJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "phone_number", unique = true, nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "presence_status", nullable = false, length = 30)
    private PresenceStatus presenceStatus;

    @Column(name = "is_premium", nullable = false)
    private boolean premium;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Factory method de construction (pas de constructeur public)
    public static UserJpaEntity of(
            UUID id,
            String phoneNumber,
            String countryCode,
            String firstName,
            String lastName,
            String avatarUrl,
            PresenceStatus presenceStatus,
            boolean premium,
            String passwordHash,
            Instant createdAt
    ) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.id = id;
        entity.phoneNumber = phoneNumber;
        entity.countryCode = countryCode;
        entity.firstName = firstName;
        entity.lastName = lastName;
        entity.avatarUrl = avatarUrl;
        entity.presenceStatus = presenceStatus;
        entity.premium = premium;
        entity.passwordHash = passwordHash;
        entity.createdAt = createdAt;
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserJpaEntity u)) return false;
        return id != null && id.equals(u.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
