package com.uzima.application.social;

import com.uzima.application.social.port.in.CreateCircleCommand;
import com.uzima.application.social.port.out.CircleRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.social.model.Circle;
import com.uzima.domain.social.model.CircleId;

import java.util.Objects;

/**
 * Use Case : Création d'un Cercle de Vie.
 * <p>
 * Orchestration :
 * 1. Instancier Circle via Circle.create() (règles par défaut + OWNER ajouté automatiquement)
 * 2. Persister le cercle
 * 3. Retourner le CircleId
 * <p>
 * La logique métier (nom valide, règles par type) reste dans l'agrégat Circle.
 */
public final class CreateCircleUseCase {

    private final CircleRepositoryPort circleRepository;
    private final TimeProvider         clock;

    public CreateCircleUseCase(CircleRepositoryPort circleRepository, TimeProvider clock) {
        this.circleRepository = Objects.requireNonNull(circleRepository, "Le repository de cercles est obligatoire");
        this.clock            = Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
    }

    /**
     * @return L'identifiant du cercle créé
     * @throws Circle.InvalidCircleNameException si le nom est invalide
     */
    public CircleId execute(CreateCircleCommand command) {
        Objects.requireNonNull(command, "La commande de création est obligatoire");

        Circle circle = Circle.create(
            command.name(),
            command.type(),
            command.requesterId(),
            clock
        );

        circleRepository.save(circle);
        return circle.id();
    }
}
