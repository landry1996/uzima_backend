package com.uzima.application.message;

import com.uzima.application.message.port.out.MessageRepositoryPort;
import com.uzima.application.message.port.out.TranslationPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.message.model.MessageId;
import com.uzima.domain.message.model.MessageMetadata;

import java.util.Objects;

/** Use Case : Traduire le contenu d'un message dans une langue cible. */
public class TranslateMessageUseCase {

    private final MessageRepositoryPort messageRepository;
    private final TranslationPort       translationPort;

    public TranslateMessageUseCase(MessageRepositoryPort messageRepository,
                                    TranslationPort translationPort) {
        this.messageRepository = Objects.requireNonNull(messageRepository);
        this.translationPort   = Objects.requireNonNull(translationPort);
    }

    /**
     * @param messageId      Identifiant du message à traduire
     * @param targetLanguage Code langue ISO 639-1 (ex. "fr", "en")
     * @return Texte traduit
     */
    public String execute(MessageId messageId, String targetLanguage) {
        Objects.requireNonNull(messageId,      "messageId est obligatoire");
        Objects.requireNonNull(targetLanguage, "targetLanguage est obligatoire");

        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> ResourceNotFoundException.messageNotFound(messageId));

        // Traduit le texte brut (ou la transcription si disponible)
        String sourceText = message.metadata()
            .flatMap(MessageMetadata::transcription)
            .orElse(message.content().text());

        String translated = translationPort.translate(sourceText, targetLanguage);

        message.enrich(MessageMetadata.withTranslation(translated, targetLanguage));
        messageRepository.save(message);

        return translated;
    }
}
