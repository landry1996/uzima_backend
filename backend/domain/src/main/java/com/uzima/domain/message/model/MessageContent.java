package com.uzima.domain.message.model;

import com.uzima.domain.shared.DomainException;

import java.util.Objects;

/**
 * Value Object : Contenu d'un message texte.
 * Invariants :
 * - Non nul, non vide (après trim)
 * - Longueur maximale : 4096 caractères (compatible WhatsApp/Signal)
 */
public record MessageContent(String text) {

    private static final int MAX_LENGTH = 4096;

    public MessageContent {
        Objects.requireNonNull(text, "Le contenu du message ne peut pas être nul");
        if (text.isBlank()) {
            throw new EmptyMessageContentException("Le message ne peut pas être vide");
        }
        if (text.length() > MAX_LENGTH) {
            throw new MessageTooLongException(
                "Le message dépasse la limite de " + MAX_LENGTH + " caractères (reçu : " + text.length() + ")"
            );
        }
    }

    public static MessageContent of(String text) {
        return new MessageContent(text);
    }

    public int length() {
        return text.length();
    }

    @Override
    public String toString() {
        return text;
    }

    public static final class EmptyMessageContentException extends DomainException {
        public EmptyMessageContentException(String message) {
            super(message);
        }
    }

    public static final class MessageTooLongException extends DomainException {
        public MessageTooLongException(String message) {
            super(message);
        }
    }
}
