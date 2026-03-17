package com.uzima.application.social;

import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.social.port.in.RenameCircleCommand;
import com.uzima.application.social.port.out.CircleRepositoryPort;
import com.uzima.domain.social.model.Circle;

import java.util.Objects;

/**
 * Use Case : Renommage d'un Cercle de Vie.
 * <p>
 * La vérification des permissions (ADMIN+) et la validation du nom
 * sont déléguées à Circle.rename().
 */
public final class RenameCircleUseCase {

    private final CircleRepositoryPort circleRepository;

    public RenameCircleUseCase(CircleRepositoryPort circleRepository) {
        this.circleRepository = Objects.requireNonNull(circleRepository, "Le repository de cercles est obligatoire");
    }

    /**
     * @throws ResourceNotFoundException              si le cercle n'existe pas
     * @throws Circle.InsufficientPermissionException si le demandeur n'est pas ADMIN+
     * @throws Circle.InvalidCircleNameException      si le nouveau nom est invalide
     */
    public void execute(RenameCircleCommand command) {
        Objects.requireNonNull(command, "La commande de renommage est obligatoire");

        Circle circle = circleRepository.findById(command.circleId())
                .orElseThrow(() -> ResourceNotFoundException.circleNotFound(command.circleId()));

        circle.rename(command.newName(), command.requesterId());
        circleRepository.save(circle);
    }
}
