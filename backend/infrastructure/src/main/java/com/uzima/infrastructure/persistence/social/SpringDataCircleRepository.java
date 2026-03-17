package com.uzima.infrastructure.persistence.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository Spring Data JPA interne : circles.
 * Package-private : ne doit pas être utilisé directement hors de l'infrastructure.
 */
public interface SpringDataCircleRepository extends JpaRepository<CircleJpaEntity, UUID> {

    /** Cercles dont l'utilisateur est propriétaire (OWNER). */
    List<CircleJpaEntity> findByOwnerId(UUID ownerId);

    /**
     * Cercles dont l'utilisateur est membre (quel que soit son rôle).
     * Joint sur circle_memberships pour trouver les cercles par member_id.
     */
    @Query("SELECT DISTINCT c FROM CircleJpaEntity c JOIN c.memberships m WHERE m.memberId = :memberId")
    List<CircleJpaEntity> findByMemberId(@Param("memberId") UUID memberId);
}
