package com.uzima.application.user;

import com.uzima.application.user.port.in.UpdateUserProfileCommand;
import com.uzima.application.user.port.out.UserRepositoryPort;
import com.uzima.domain.user.model.FirstName;
import com.uzima.domain.user.model.LastName;
import com.uzima.domain.user.model.User;

import java.util.Objects;

/**
 * Use Case : Mise à jour du profil utilisateur.
 * <p>
 * Justifie l'existence de :
 * - User.changeName(FirstName, LastName)
 * - User.updateAvatarUrl(String)
 * <p>
 * Règles :
 * - Au moins un champ doit changer (sinon no-op silencieux)
 * - La validation du format des noms est déléguée aux Value Objects FirstName/LastName
 * - L'URL de l'avatar n'est pas validée ici (préoccupation infrastructure/CDN)
 * <p>
 * Pas de Spring. Pas de framework.
 */
public final class UpdateUserProfileUseCase {

    private final UserRepositoryPort userRepository;

    public UpdateUserProfileUseCase(UserRepositoryPort userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    /**
     * @param command Les champs à mettre à jour (null = non modifié)
     * @return L'utilisateur mis à jour
     * @throws UserNotFoundException si l'utilisateur n'existe pas
     * @throws FirstName.InvalidFirstNameException si le prénom est invalide
     * @throws LastName.InvalidLastNameException si le nom est invalide
     */
    public User execute(UpdateUserProfileCommand command) {
        Objects.requireNonNull(command, "La commande ne peut pas être nulle");

        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(
                    "Utilisateur introuvable : " + command.userId()
                ));

        // Mise à jour du nom — User.changeName() justifié ici
        if (command.firstName() != null && command.lastName() != null) {
            user.changeName(FirstName.of(command.firstName()), LastName.of(command.lastName()));
        }

        // Mise à jour de l'avatar — User.updateAvatarUrl() justifié ici
        if (command.avatarUrl() != null) {
            user.updateAvatarUrl(command.avatarUrl());
        }

        userRepository.save(user);
        return user;
    }

    public static final class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) { super(message); }
    }
}
