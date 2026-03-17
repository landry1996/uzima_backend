package com.uzima.domain.social.model;

import com.uzima.domain.user.model.UserId;

import java.time.Instant;
import java.util.Objects;

/**
 * Entity : Membership d'un utilisateur dans un Cercle de Vie.
 * N'est pas un Aggregate Root — appartient au cycle de vie de Circle.
 * Toute mutation passe par Circle (addMember, removeMember, promoteToAdmin…).
 * Immuable après création : le rôle ne change que via Circle.updateMemberRole().
 */
public final class CircleMembership {

    private final UserId    memberId;
    private       MemberRole role;
    private final Instant   joinedAt;

    // -------------------------------------------------------------------------
    // Constructeur package-private : instancié uniquement par Circle
    // -------------------------------------------------------------------------

    CircleMembership(UserId memberId, MemberRole role, Instant joinedAt) {
        this.memberId = Objects.requireNonNull(memberId, "L'identifiant du membre est obligatoire");
        this.role     = Objects.requireNonNull(role,     "Le rôle est obligatoire");
        this.joinedAt = Objects.requireNonNull(joinedAt, "La date d'adhésion est obligatoire");
    }

    // -------------------------------------------------------------------------
    // Factory de reconstitution (infrastructure uniquement)
    // -------------------------------------------------------------------------

    /**
     * Reconstitue un membership depuis la persistance.
     * Réservé aux mappers infrastructure — ne pas utiliser dans la logique métier.
     * Pour créer un membership, passer par {@link Circle#addMember}.
     */
    public static CircleMembership reconstitute(UserId memberId, MemberRole role, Instant joinedAt) {
        return new CircleMembership(memberId, role, joinedAt);
    }

    // -------------------------------------------------------------------------
    // Accesseurs
    // -------------------------------------------------------------------------

    public UserId    memberId() { return memberId; }
    public MemberRole role()    { return role; }
    public Instant   joinedAt() { return joinedAt; }

    // -------------------------------------------------------------------------
    // Prédicats
    // -------------------------------------------------------------------------

    public boolean isOwner()   { return role == MemberRole.OWNER; }
    public boolean isAdmin()   { return role.isAtLeastAdmin(); }
    public boolean isMember(UserId userId) { return memberId.equals(userId); }

    // -------------------------------------------------------------------------
    // Mutation package-private (appelée uniquement par Circle)
    // -------------------------------------------------------------------------

    void updateRole(MemberRole newRole) {
        Objects.requireNonNull(newRole, "Le nouveau rôle est obligatoire");
        this.role = newRole;
    }

    // -------------------------------------------------------------------------
    // equals / hashCode (identité par memberId — unique dans un cercle donné)
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CircleMembership m)) return false;
        return memberId.equals(m.memberId);
    }

    @Override
    public int hashCode() {
        return memberId.hashCode();
    }

    @Override
    public String toString() {
        return "CircleMembership{memberId=" + memberId + ", role=" + role.displayName() + "}";
    }
}
