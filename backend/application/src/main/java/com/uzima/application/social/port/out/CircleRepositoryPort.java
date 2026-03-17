package com.uzima.application.social.port.out;

import com.uzima.domain.social.model.Circle;
import com.uzima.domain.social.model.CircleId;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Port OUT (application) : Persistance des Cercles de Vie.
 * <p>
 * Miroir du port domaine CircleRepository.
 * Implémenté par CircleRepositoryAdapter dans l'infrastructure.
 */
public interface CircleRepositoryPort {

    void save(Circle circle);

    Optional<Circle> findById(CircleId id);

    List<Circle> findByMemberId(UserId memberId);

    List<Circle> findByOwnerId(UserId ownerId);
}
