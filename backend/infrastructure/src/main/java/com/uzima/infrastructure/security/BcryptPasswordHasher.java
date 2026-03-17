package com.uzima.infrastructure.security;

import com.uzima.application.user.port.out.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Adaptateur : Implémentation du PasswordHasherPort avec BCrypt.
 * <p>
 * L'application ne connaît pas BCrypt. Elle utilise le port.
 * BCrypt est confiné ici, dans l'infrastructure.
 */
public final class BcryptPasswordHasher implements PasswordHasherPort {

    private final BCryptPasswordEncoder encoder;

    public BcryptPasswordHasher() {
        this.encoder = new BCryptPasswordEncoder(12); // Strength 12 = bon équilibre sécurité/performance
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword);
    }
}
