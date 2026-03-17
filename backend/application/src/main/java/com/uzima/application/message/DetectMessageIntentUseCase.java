package com.uzima.application.message;

import com.uzima.application.message.port.out.IntentDetectionPort;
import com.uzima.application.message.port.out.IntentDetectionPort.DetectedIntent;
import com.uzima.application.message.port.out.MessageRepositoryPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.message.model.MessageId;
import com.uzima.domain.message.model.MessageMetadata;

import java.util.Objects;

/** Use Case : Détecter l'intention dans un message texte (NLP). */
public class DetectMessageIntentUseCase {

    private final MessageRepositoryPort messageRepository;
    private final IntentDetectionPort   intentDetectionPort;

    public DetectMessageIntentUseCase(MessageRepositoryPort messageRepository,
                                       IntentDetectionPort intentDetectionPort) {
        this.messageRepository   = Objects.requireNonNull(messageRepository);
        this.intentDetectionPort = Objects.requireNonNull(intentDetectionPort);
    }

    public DetectedIntent execute(MessageId messageId) {
        Objects.requireNonNull(messageId, "messageId est obligatoire");

        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> ResourceNotFoundException.messageNotFound(messageId));

        // Analyse le texte brut ou la transcription si disponible
        String textToAnalyze = message.metadata()
            .flatMap(MessageMetadata::transcription)
            .orElse(message.content().text());

        DetectedIntent result = intentDetectionPort.detect(textToAnalyze);

        message.enrich(MessageMetadata.withIntent(result.intent()));
        messageRepository.save(message);

        return result;
    }
}
