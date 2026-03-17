package com.uzima.application.social;

import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.application.social.port.in.AddMemberCommand;
import com.uzima.application.social.port.out.CircleRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.social.model.Circle;

import java.util.Objects;

/**
 * Use Case : Ajout d'un membre dans un Cercle de Vie.
 * <p>
 * Orchestration :
 * 1. Charger le cercle (404 si inexistant)
 * 2. Vérifier que le demandeur est ADMIN+ (403 sinon)
 * 3. Déléguer l'ajout à Circle.addMember() (invariants domaine : pas de doublon, pas d'OWNER)
 * 4. Persister
 */
public final class AddMemberToCircleUseCase {

    private final CircleRepositoryPort circleRepository;
    private final TimeProvider         clock;

    public AddMemberToCircleUseCase(CircleRepositoryPort circleRepository, TimeProvider clock) {
        this.circleRepository = Objects.requireNonNull(circleRepository, "Le repository de cercles est obligatoire");
        this.clock            = Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
    }

    /**
     * @throws ResourceNotFoundException       si le cercle n'existe pas
     * @throws CircleAccessDeniedException     si le demandeur n'est pas ADMIN+
     * @throws Circle.DuplicateMemberException si le membre est déjà dans le cercle
     */
    public void execute(AddMemberCommand command) {
        Objects.requireNonNull(command, "La commande d'ajout est obligatoire");

        Circle circle = circleRepository.findById(command.circleId())
                .orElseThrow(() -> ResourceNotFoundException.circleNotFound(command.circleId()));

        if (!circle.isAdmin(command.requesterId())) {
            throw new CircleAccessDeniedException(
                "Seul un ADMIN ou OWNER peut ajouter des membres au cercle " + command.circleId()
            );
        }

        circle.addMember(command.newMemberId(), command.role(), clock);
        circleRepository.save(circle);
    }

    // -------------------------------------------------------------------------
    // Exceptions applicatives
    // -------------------------------------------------------------------------

    /** Le demandeur n'a pas les droits suffisants sur le cercle. HTTP 403. */
    public static final class CircleAccessDeniedException extends UnauthorizedException {
        public CircleAccessDeniedException(String message) {
            super("CIRCLE_ACCESS_DENIED", message);
        }
    }
}
