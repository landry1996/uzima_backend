package com.uzima.application.user;

import com.uzima.application.user.port.in.UpdatePresenceStatusCommand;
import com.uzima.application.user.port.out.UserRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.User;

import java.util.Objects;

/**
 * Use Case : Mise à jour de l'état de présence (Nouveauté 6 & 8 du cahier des charges).
 */
public final class UpdatePresenceStatusUseCase {

    private final UserRepositoryPort userRepository;
    private final TimeProvider clock;

    public UpdatePresenceStatusUseCase(UserRepositoryPort userRepository, TimeProvider clock) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public void execute(UpdatePresenceStatusCommand command) {
        Objects.requireNonNull(command, "La commande ne peut pas être nulle");

        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable : " + command.userId()));

        user.updatePresenceStatus(command.newStatus(), clock);
        userRepository.save(user);
    }

    public static final class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }
}
