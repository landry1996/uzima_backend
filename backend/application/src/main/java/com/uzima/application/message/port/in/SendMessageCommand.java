package com.uzima.application.message.port.in;

import com.uzima.domain.message.model.ConversationId;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Commande d'entrée : Envoi d'un message.
 * isVoiceMessage() permet au use case de déclencher la vérification
 * de PresenceStatus.allowsVoiceMessages() pour les destinataires.
 */
public record SendMessageCommand(
        ConversationId conversationId,
        UserId senderId,
        String textContent,
        boolean isVoiceMessage
) {
    public SendMessageCommand {
        Objects.requireNonNull(conversationId, "L'identifiant de conversation est obligatoire");
        Objects.requireNonNull(senderId, "L'identifiant de l'expéditeur est obligatoire");
        Objects.requireNonNull(textContent, "Le contenu est obligatoire");
    }

    /** Constructeur de convenance pour les messages texte (cas le plus fréquent). */
    public static SendMessageCommand text(ConversationId conversationId, UserId senderId, String content) {
        return new SendMessageCommand(conversationId, senderId, content, false);
    }

    /** Constructeur de convenance pour les messages vocaux. */
    public static SendMessageCommand voice(ConversationId conversationId, UserId senderId, String audioUrl) {
        return new SendMessageCommand(conversationId, senderId, audioUrl, true);
    }
}
