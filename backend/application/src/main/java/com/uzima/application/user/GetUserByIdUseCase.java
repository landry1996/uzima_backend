package com.uzima.application.user;

import com.uzima.application.user.port.out.UserRepositoryPort;
import com.uzima.domain.user.model.User;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Use Case (Query) : Récupérer un utilisateur par son identifiant.
 * <p>
 * Utilisé par :
 * - {@code GET /api/users/me}  → profil de l'utilisateur courant
 * - {@code GET /api/users/{id}} → profil public d'un autre utilisateur
 */
public final class GetUserByIdUseCase {

    private final UserRepositoryPort userRepository;

    public GetUserByIdUseCase(UserRepositoryPort userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    /**
     * @param userId Identifiant de l'utilisateur à récupérer
     * @return L'utilisateur trouvé
     * @throws UserNotFoundException si l'utilisateur n'existe pas
     */
    public User execute(UserId userId) {
        Objects.requireNonNull(userId, "L'identifiant utilisateur est obligatoire");
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable : " + userId));
    }

    public static final class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) { super(message); }
    }
}
