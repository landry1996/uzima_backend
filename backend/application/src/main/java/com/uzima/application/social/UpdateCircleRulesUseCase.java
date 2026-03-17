package com.uzima.application.social;

import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.social.port.in.UpdateCircleRulesCommand;
import com.uzima.application.social.port.out.CircleRepositoryPort;
import com.uzima.domain.social.model.Circle;

import java.util.Objects;

/**
 * Use Case : Mise à jour des règles d'un Cercle de Vie.
 * <p>
 * La vérification des permissions (ADMIN+) est délégée à Circle.updateRules()
 * qui lève InsufficientPermissionException si le demandeur n'a pas les droits.
 */
public final class UpdateCircleRulesUseCase {

    private final CircleRepositoryPort circleRepository;

    public UpdateCircleRulesUseCase(CircleRepositoryPort circleRepository) {
        this.circleRepository = Objects.requireNonNull(circleRepository, "Le repository de cercles est obligatoire");
    }

    /**
     * @throws ResourceNotFoundException            si le cercle n'existe pas
     * @throws Circle.InsufficientPermissionException si le demandeur n'est pas ADMIN+
     * @throws Circle.MemberNotFoundException         si le demandeur n'est pas membre
     */
    public void execute(UpdateCircleRulesCommand command) {
        Objects.requireNonNull(command, "La commande de mise à jour des règles est obligatoire");

        Circle circle = circleRepository.findById(command.circleId())
                .orElseThrow(() -> ResourceNotFoundException.circleNotFound(command.circleId()));

        circle.updateRules(command.newRules(), command.requesterId());
        circleRepository.save(circle);
    }
}
