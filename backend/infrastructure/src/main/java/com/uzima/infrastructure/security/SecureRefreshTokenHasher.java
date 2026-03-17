package com.uzima.infrastructure.security;

import com.uzima.security.token.port.RefreshTokenHasherPort;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Adaptateur : Hachage sécurisé des refresh tokens (SHA-256).
 * Implémente RefreshTokenHasherPort du module security.
 * Les refresh tokens sont des valeurs aléatoires, pas des JWTs.
 * Stockage en base sous forme hachée pour éviter les fuites.
 */
public final class SecureRefreshTokenHasher implements RefreshTokenHasherPort {

    private static final int TOKEN_BYTES = 32; // 256 bits → 64 hex chars
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateRawValue() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    @Override
    public String hash(String rawValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawValue.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 est garanti par la JVM (depuis Java 7)
            throw new IllegalStateException("SHA-256 non disponible", e);
        }
    }
}
