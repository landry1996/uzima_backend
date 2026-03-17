package com.uzima.bootstrap.adapter.http;

import com.uzima.application.user.AuthenticateUserUseCase;
import com.uzima.application.user.RegisterUserUseCase;
import com.uzima.bootstrap.adapter.http.dto.LoginRequest;
import com.uzima.bootstrap.adapter.http.dto.LoginResponse;
import com.uzima.bootstrap.adapter.http.dto.RegisterRequest;
import com.uzima.bootstrap.adapter.http.dto.UserResponse;
import com.uzima.bootstrap.adapter.http.mapper.UserHttpMapper;
import com.uzima.domain.user.model.User;
import com.uzima.security.token.model.TokenPair;
import com.uzima.security.token.usecase.GenerateTokenPairUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * Adaptateur HTTP entrant : Authentification.
 * <p>
 * Responsabilités strictes :
 * - Recevoir les requêtes HTTP (liaison de données)
 * - Déléguer aux Use Cases via UserHttpMapper
 * - Orchestrer authenticate → generate token pair (rôle légitime du bootstrap)
 * - Retourner les réponses HTTP
 * <p>
 * Ce contrôleur NE CONTIENT :
 * - Aucune logique métier
 * - Aucun @ExceptionHandler (centralisé dans GlobalExceptionHandler)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final GenerateTokenPairUseCase generateTokenPairUseCase;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            AuthenticateUserUseCase authenticateUserUseCase,
            GenerateTokenPairUseCase generateTokenPairUseCase
    ) {
        this.registerUserUseCase = Objects.requireNonNull(registerUserUseCase);
        this.authenticateUserUseCase = Objects.requireNonNull(authenticateUserUseCase);
        this.generateTokenPairUseCase = Objects.requireNonNull(generateTokenPairUseCase);
    }

    /**
     * POST /api/auth/register
     * 201 Created + UserResponse
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        var command = UserHttpMapper.toRegisterCommand(request);
        var user = registerUserUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserHttpMapper.toUserResponse(user));
    }

    /**
     * POST /api/auth/login
     * Orchestre : authentification → génération de la paire access+refresh token.
     * 200 OK + LoginResponse (accessToken + refreshToken + profil)
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var command = UserHttpMapper.toAuthCommand(request);
        User user = authenticateUserUseCase.execute(command);
        TokenPair tokenPair = generateTokenPairUseCase.execute(user.id());
        return ResponseEntity.ok(UserHttpMapper.toLoginResponse(user, tokenPair));
    }
}
