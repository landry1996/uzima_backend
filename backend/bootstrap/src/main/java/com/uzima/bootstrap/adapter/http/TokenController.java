package com.uzima.bootstrap.adapter.http;

import com.uzima.bootstrap.adapter.http.dto.TokenRefreshRequest;
import com.uzima.bootstrap.adapter.http.dto.TokenResponse;
import com.uzima.domain.user.model.UserId;
import com.uzima.security.token.model.AccessToken;
import com.uzima.security.token.model.RefreshToken;
import com.uzima.security.token.model.TokenPair;
import com.uzima.security.token.usecase.GetActiveSessionsUseCase;
import com.uzima.security.token.usecase.IntrospectTokenUseCase;
import com.uzima.security.token.usecase.RefreshAccessTokenUseCase;
import com.uzima.security.token.usecase.RevokeAllTokensUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Contrôleur HTTP : Gestion des tokens OAuth2-like.
 * Endpoints :
 * - POST /api/auth/token/refresh      → Rotation du refresh token (sans authentification)
 * - POST /api/auth/token/revoke       → Révocation globale (nécessite un access token valide)
 * - GET  /api/auth/token/introspect   → Introspection de l'access token courant
 * - GET  /api/auth/token/sessions     → Sessions actives de l'utilisateur courant
 * <p>
 * La protection CSRF n'est pas appliquée ici car les clients mobiles/SPA utilisent des tokens.
 * Les exceptions sont gérées par GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/auth/token")
public class TokenController {

    private final RefreshAccessTokenUseCase  refreshAccessTokenUseCase;
    private final RevokeAllTokensUseCase     revokeAllTokensUseCase;
    private final IntrospectTokenUseCase     introspectTokenUseCase;
    private final GetActiveSessionsUseCase   getActiveSessionsUseCase;

    public TokenController(
            RefreshAccessTokenUseCase  refreshAccessTokenUseCase,
            RevokeAllTokensUseCase     revokeAllTokensUseCase,
            IntrospectTokenUseCase     introspectTokenUseCase,
            GetActiveSessionsUseCase   getActiveSessionsUseCase
    ) {
        this.refreshAccessTokenUseCase = refreshAccessTokenUseCase;
        this.revokeAllTokensUseCase    = revokeAllTokensUseCase;
        this.introspectTokenUseCase    = introspectTokenUseCase;
        this.getActiveSessionsUseCase  = getActiveSessionsUseCase;
    }

    /**
     * Rotation du refresh token.
     * Échange un refresh token valide contre une nouvelle paire access+refresh.
     * L'ancien refresh token est immédiatement révoqué.
     * Si le token présenté est déjà révoqué → replay attack → toute la famille est révoquée.
     * <p>
     * POST /api/auth/token/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        TokenPair tokenPair = refreshAccessTokenUseCase.execute(request.refreshToken());
        return ResponseEntity.ok(TokenResponse.from(tokenPair));
    }

    /**
     * Révocation globale (logout de tous les appareils).
     * Nécessite un access token valide dans Authorization: Bearer.
     * Tous les refresh tokens actifs de l'utilisateur sont révoqués.
     * <p>
     * POST /api/auth/token/revoke
     */
    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeAll(@AuthenticationPrincipal String userId) {
        revokeAllTokensUseCase.execute(UserId.of(userId));
        return ResponseEntity.noContent().build();
    }

    /**
     * Introspection de l'access token courant.
     * Renvoie les métadonnées du token (userId, dates d'émission/expiration).
     * Lance {@link com.uzima.security.token.port.AccessTokenVerifierPort.InvalidTokenException}
     * si le token est invalide → GlobalExceptionHandler → HTTP 401.
     * <p>
     * GET /api/auth/token/introspect
     * Authorization: Bearer <access_token>
     */
    @GetMapping("/introspect")
    public ResponseEntity<TokenIntrospectionResponse> introspect(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        String rawToken = authorizationHeader.startsWith("Bearer ")
            ? authorizationHeader.substring(7)
            : authorizationHeader;
        AccessToken token = introspectTokenUseCase.execute(rawToken);
        return ResponseEntity.ok(TokenIntrospectionResponse.from(token));
    }

    /**
     * Sessions actives de l'utilisateur courant.
     * Chaque session correspond à un refresh token non révoqué et non expiré.
     * <p>
     * GET /api/auth/token/sessions
     * Authorization: Bearer <access_token>
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> activeSessions(
            @AuthenticationPrincipal String userId
    ) {
        List<RefreshToken> sessions = getActiveSessionsUseCase.execute(UserId.of(userId));
        return ResponseEntity.ok(sessions.stream().map(SessionResponse::from).toList());
    }

    // -------------------------------------------------------------------------
    // DTOs
    // -------------------------------------------------------------------------

    record TokenIntrospectionResponse(
            String tokenId,
            String userId,
            Instant issuedAt,
            Instant expiresAt
    ) {
        static TokenIntrospectionResponse from(AccessToken t) {
            return new TokenIntrospectionResponse(
                t.tokenId().toString(),
                t.userId().toString(),
                t.issuedAt(),
                t.expiresAt()
            );
        }
    }

    record SessionResponse(
            String tokenId,
            String familyId,
            Instant issuedAt,
            Instant expiresAt
    ) {
        static SessionResponse from(RefreshToken t) {
            return new SessionResponse(
                t.tokenId().toString(),
                t.family().toString(),
                t.issuedAt(),
                t.expiresAt()
            );
        }
    }
}
