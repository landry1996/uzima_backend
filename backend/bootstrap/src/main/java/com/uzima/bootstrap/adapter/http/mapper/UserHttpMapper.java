package com.uzima.bootstrap.adapter.http.mapper;

import com.uzima.application.user.port.in.AuthenticateUserCommand;
import com.uzima.application.user.port.in.RegisterUserCommand;
import com.uzima.bootstrap.adapter.http.dto.*;
import com.uzima.domain.user.model.User;
import com.uzima.security.token.model.TokenPair;

/**
 * Mapper HTTP : Conversion entre DTOs HTTP et objets applicatifs/domaine.
 * <p>
 * Responsabilités :
 * - DTO → Command : transformation des requêtes HTTP entrantes
 * - Domain → DTO : transformation des réponses sortantes
 *
 * Règles du mapper :
 * - Pas de logique métier (pas de validation sémantique)
 * - Correspondance structurelle uniquement
 */
public final class UserHttpMapper {

    private UserHttpMapper() {}

    // -------------------------------------------------------------------------
    // DTO → Command (HTTP → Application)
    // -------------------------------------------------------------------------

    public static RegisterUserCommand toRegisterCommand(RegisterRequest request) {
        return new RegisterUserCommand(
                request.phoneNumber(),
                request.countryCode(),
                request.firstName(),
                request.lastName(),
                request.password()
        );
    }

    public static AuthenticateUserCommand toAuthCommand(LoginRequest request) {
        return new AuthenticateUserCommand(
                request.phoneNumber(),
                request.password()
        );
    }

    // -------------------------------------------------------------------------
    // Domain → DTO (Application → HTTP)
    // -------------------------------------------------------------------------

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.id().toString(),
                user.phoneNumber().value(),
                user.country().value(),
                user.firstName().value(),
                user.lastName().value(),
                user.avatarUrl().orElse(null),
                user.presenceStatus().name(),
                user.isPremium()
        );
    }

    public static LoginResponse toLoginResponse(User user, TokenPair tokenPair) {
        return new LoginResponse(
                tokenPair.accessToken().rawValue(),
                tokenPair.refreshToken().hashedValue(), // NOTE: this is actually the RAW value (client view)
                user.id().toString(),
                user.firstName().value(),
                user.lastName().value(),
                user.phoneNumber().value()
        );
    }
}
