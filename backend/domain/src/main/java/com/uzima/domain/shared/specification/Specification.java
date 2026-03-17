package com.uzima.domain.shared.specification;

/**
 * Pattern Specification (Eric Evans - DDD).
 * Encapsule une règle métier réutilisable et composable.
 * Avantages :
 * - Nommage explicite des règles (ex: QrCodeIsActiveSpecification)
 * - Composabilité (and/or/not) sans duplication de code
 * - Testabilité unitaire indépendante
 * - Élimination des if/else complexes dans les agrégats et use cases
 * Usage :
 *   Specification<QrCode> spec = new QrCodeIsActiveSpecification(now)
 *       .and(new QrCodeBelongsToUserSpecification(userId));
 *   boolean valid = spec.isSatisfiedBy(qrCode);
 *
 * @param <T> Le type de l'objet vérifié
 */
public interface Specification<T> {

    /**
     * Vérifie si le candidat satisfait cette spécification.
     */
    boolean isSatisfiedBy(T candidate);

    /**
     * Compose avec une autre spécification par ET logique.
     */
    default Specification<T> and(Specification<T> other) {
        return candidate -> this.isSatisfiedBy(candidate) && other.isSatisfiedBy(candidate);
    }

    /**
     * Compose avec une autre spécification par OU logique.
     */
    default Specification<T> or(Specification<T> other) {
        return candidate -> this.isSatisfiedBy(candidate) || other.isSatisfiedBy(candidate);
    }

    /**
     * Inverse cette spécification (NOT logique).
     */
    default Specification<T> not() {
        return candidate -> !this.isSatisfiedBy(candidate);
    }
}
