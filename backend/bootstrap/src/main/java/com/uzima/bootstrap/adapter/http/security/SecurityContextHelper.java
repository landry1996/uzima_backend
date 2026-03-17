package com.uzima.bootstrap.adapter.http.security;

import com.uzima.domain.user.model.UserId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utilitaire : Extraction de l'identité depuis le SecurityContext JWT.
 * <p>
 * Le {@link com.uzima.bootstrap.adapter.http.security.JwtAuthenticationFilter} stocke
 * le {@code userId} (String UUID) comme principal dans l'Authentication.
 * Cette classe centralise l'extraction pour tous les contrôleurs.
 * <p>
 * Usage dans un contrôleur :
 * <pre>
 *     UserId userId = SecurityContextHelper.currentUserId();
 * </pre>
 */
public final class SecurityContextHelper {

    private SecurityContextHelper() {}

    /**
     * Retourne l'identifiant de l'utilisateur authentifié courant.
     *
     * @return UserId extrait du token JWT
     * @throws UnauthenticatedException si aucune authentification n'est présente dans le contexte
     */
    public static UserId currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new UnauthenticatedException("Aucun utilisateur authentifié dans le contexte courant");
        }
        String principal = auth.getPrincipal().toString();
        return UserId.of(principal);
    }

    /**
     * Exception levée lorsque le contexte de sécurité ne contient pas d'authentification valide.
     * Traduite en HTTP 401 par le GlobalExceptionHandler.
     */
    public static final class UnauthenticatedException extends RuntimeException {
        public UnauthenticatedException(String message) {
            super(message);
        }
    }
}
