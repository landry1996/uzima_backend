package com.uzima.application.user.port.out;

/**
 * Port de sortie : hachage et vérification de mots de passe.
 * <p>
 * Le domaine et l'application NE CONNAISSENT PAS BCrypt.
 * L'implémentation concrète (BCrypt, Argon2, etc.) est dans l'infrastructure.
 */
public interface PasswordHasherPort {

    /**
     * Hache un mot de passe brut.
     *
     * @param rawPassword Le mot de passe en clair
     * @return Le hash sécurisé (non réversible)
     */
    String hash(String rawPassword);

    /**
     * Vérifie si un mot de passe brut correspond à un hash stocké.
     *
     * @param rawPassword   Le mot de passe en clair à vérifier
     * @param hashedPassword Le hash stocké
     * @return true si le mot de passe correspond
     */
    boolean matches(String rawPassword, String hashedPassword);
}
