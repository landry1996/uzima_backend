package com.uzima.application.social;

import com.uzima.application.social.port.out.CircleRepositoryPort;
import com.uzima.domain.social.model.Circle;
import com.uzima.domain.social.model.CircleType;
import com.uzima.domain.user.model.UserId;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Use Case (Query) : Suggestion du cercle le plus adapté pour un contact.
 * <p>
 * Heuristiques MVP (pas de ML) :
 * 1. Si le contact est déjà dans un cercle commun → suggérer ce type (par fréquence)
 * 2. Si le contact n'est dans aucun cercle commun → CLOSE_FRIENDS par défaut
 * <p>
 * L'objectif est d'aider l'utilisateur à classer rapidement un nouveau contact
 * sans avoir à réfléchir au type de cercle approprié.
 */
public final class SuggestCircleForContactUseCase {

    private final CircleRepositoryPort circleRepository;

    public SuggestCircleForContactUseCase(CircleRepositoryPort circleRepository) {
        this.circleRepository = Objects.requireNonNull(circleRepository, "Le repository de cercles est obligatoire");
    }

    /**
     * @param requesterId Identifiant de l'utilisateur qui demande la suggestion
     * @param contactId   Identifiant du contact à classifier
     * @return Suggestion avec le type recommandé et une raison lisible
     */
    public CircleSuggestion execute(UserId requesterId, UserId contactId) {
        Objects.requireNonNull(requesterId, "L'identifiant du demandeur est obligatoire");
        Objects.requireNonNull(contactId,   "L'identifiant du contact est obligatoire");

        List<Circle> myCircles      = circleRepository.findByMemberId(requesterId);
        List<Circle> contactCircles = circleRepository.findByMemberId(contactId);

        // Cercles communs entre l'utilisateur et le contact
        List<CircleType> sharedTypes = myCircles.stream()
                .filter(c -> contactCircles.stream()
                        .anyMatch(cc -> cc.id().equals(c.id())))
                .map(Circle::type)
                .toList();

        if (!sharedTypes.isEmpty()) {
            // Suggérer le type le plus fréquent dans les cercles communs
            CircleType mostCommon = sharedTypes.stream()
                    .collect(Collectors.groupingBy(t -> t, Collectors.counting()))
                    .entrySet().stream()
                    .max(Comparator.comparingLong(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse(CircleType.CLOSE_FRIENDS);

            return new CircleSuggestion(
                mostCommon,
                "Vous partagez déjà " + sharedTypes.size() + " cercle(s) de type "
                + mostCommon.displayName() + " avec ce contact"
            );
        }

        // Aucun cercle commun → CLOSE_FRIENDS par défaut
        return new CircleSuggestion(
            CircleType.CLOSE_FRIENDS,
            "Aucun cercle commun détecté — suggestion par défaut"
        );
    }

    // -------------------------------------------------------------------------
    // Vue de sortie
    // -------------------------------------------------------------------------

    /**
     * Suggestion de type de cercle pour un contact.
     *
     * @param suggestedType Type de cercle recommandé
     * @param reason        Explication lisible (pour l'UI)
     */
    public record CircleSuggestion(
            CircleType suggestedType,
            String     reason
    ) {}
}
