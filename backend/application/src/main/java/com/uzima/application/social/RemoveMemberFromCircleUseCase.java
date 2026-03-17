package com.uzima.application.social;

import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.application.social.port.in.RemoveMemberCommand;
import com.uzima.application.social.port.out.CircleRepositoryPort;
import com.uzima.application.shared.exception.UnauthorizedException;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.social.model.Circle;

import java.util.Objects;

/**
 * Use Case : Retrait d'un membre d'un Cercle de Vie.
 * <p>
 * Règles d'autorisation :
 * - Auto-retrait : tout MEMBER peut quitter lui-même (requesterId == targetMemberId)
 * - Retrait par autrui : réservé aux ADMIN et OWNER
 * <p>
 * Les invariants métier (owner ne peut pas partir) restent dans Circle.removeMember().
 */
public final class RemoveMemberFromCircleUseCase {

    private final CircleRepositoryPort circleRepository;
    private final TimeProvider         clock;

    public RemoveMemberFromCircleUseCase(CircleRepositoryPort circleRepository, TimeProvider clock) {
        this.circleRepository = Objects.requireNonNull(circleRepository, "Le repository de cercles est obligatoire");
        this.clock            = Objects.requireNonNull(clock, "Le fournisseur de temps est obligatoire");
    }

    /**
     * @throws ResourceNotFoundException        si le cercle n'existe pas
     * @throws CircleAccessDeniedException      si le demandeur ne peut pas retirer ce membre
     * @throws Circle.OwnerCannotLeaveException  si on tente de retirer l'OWNER
     * @throws Circle.MemberNotFoundException    si le membre cible n'est pas dans le cercle
     */
    public void execute(RemoveMemberCommand command) {
        Objects.requireNonNull(command, "La commande de retrait est obligatoire");

        Circle circle = circleRepository.findById(command.circleId())
                .orElseThrow(() -> ResourceNotFoundException.circleNotFound(command.circleId()));

        // Auto-retrait : autorisé pour tout membre (sauf owner — protégé par le domaine)
        // Retrait par autrui : réservé aux ADMIN+
        if (!command.isSelfRemoval() && !circle.isAdmin(command.requesterId())) {
            throw new CircleAccessDeniedException(
                "Seul un ADMIN ou OWNER peut retirer un autre membre du cercle " + command.circleId()
            );
        }

        circle.removeMember(command.targetMemberId(), clock);
        circleRepository.save(circle);
    }

    /** Le demandeur n'a pas les droits pour retirer ce membre. HTTP 403. */
    public static final class CircleAccessDeniedException extends UnauthorizedException {
        public CircleAccessDeniedException(String message) {
            super("CIRCLE_ACCESS_DENIED", message);
        }
    }
}
