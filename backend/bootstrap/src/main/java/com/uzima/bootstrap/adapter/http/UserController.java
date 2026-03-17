package com.uzima.bootstrap.adapter.http;

import com.uzima.application.user.GetUserByIdUseCase;
import com.uzima.application.user.UpdateUserProfileUseCase;
import com.uzima.application.user.port.in.UpdateUserProfileCommand;
import com.uzima.bootstrap.adapter.http.dto.UserResponse;
import com.uzima.bootstrap.adapter.http.security.SecurityContextHelper;
import com.uzima.domain.user.model.User;
import com.uzima.domain.user.model.UserId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

/**
 * Adaptateur HTTP entrant : Profil utilisateur.
 * <p>
 * Endpoints :
 * - GET /api/users/me → profil de l'utilisateur courant (JWT)
 * - PATCH /api/users/me → mise à jour du profil
 * - GET /api/users/{id} → profil public d'un autre utilisateur
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final GetUserByIdUseCase       getUserByIdUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;

    public UserController(
            GetUserByIdUseCase getUserByIdUseCase,
            UpdateUserProfileUseCase updateUserProfileUseCase
    ) {
        this.getUserByIdUseCase       = Objects.requireNonNull(getUserByIdUseCase);
        this.updateUserProfileUseCase = Objects.requireNonNull(updateUserProfileUseCase);
    }

    /**
     * GET /api/users/me
     * Retourne le profil complet de l'utilisateur authentifié.
     * 200 OK + UserResponse
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile() {
        UserId userId = SecurityContextHelper.currentUserId();
        User user = getUserByIdUseCase.execute(userId);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * PATCH /api/users/me
     * Met à jour le prénom, le nom ou l'URL d'avatar de l'utilisateur authentifié.
     * Les champs null ou absents ne sont pas modifiés.
     * 200 OK + UserResponse mis à jour
     */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(@RequestBody UpdateProfileRequest request) {
        UserId userId = SecurityContextHelper.currentUserId();
        var command = new UpdateUserProfileCommand(
                userId,
                request.firstName(),
                request.lastName(),
                request.avatarUrl()
        );
        User updated = updateUserProfileUseCase.execute(command);
        return ResponseEntity.ok(UserResponse.from(updated));
    }

    /**
     * GET /api/users/{id}
     * Retourne le profil public d'un utilisateur (pour la liste de contacts, recherche, etc.).
     * 200 OK + UserResponse
     * 404 Not Found si l'utilisateur n'existe pas
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        User user = getUserByIdUseCase.execute(UserId.of(id));
        return ResponseEntity.ok(UserResponse.from(user));
    }

    // -------------------------------------------------------------------------
    // DTOs
    // -------------------------------------------------------------------------

    /**
     * Corps de la requête PATCH /api/users/me.
     * Tous les champs sont optionnels : null signifie "ne pas modifier".
     */
    public record UpdateProfileRequest(
            String firstName,
            String lastName,
            String avatarUrl
    ) {}
}
