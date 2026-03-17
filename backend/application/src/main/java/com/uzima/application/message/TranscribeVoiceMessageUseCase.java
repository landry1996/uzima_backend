package com.uzima.application.message;

import com.uzima.application.message.port.out.MessageRepositoryPort;
import com.uzima.application.message.port.out.VoiceTranscriptionPort;
import com.uzima.application.message.port.out.VoiceTranscriptionPort.TranscriptionResult;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.message.model.MessageId;
import com.uzima.domain.message.model.MessageMetadata;

import java.util.Objects;

/**
 * Use Case : Transcrire un message vocal en texte.
 * <p>
 * Le contenu du message VOICE est l'URL de l'audio.
 * La transcription est stockée dans les métadonnées du message.
 */
public class TranscribeVoiceMessageUseCase {

    private final MessageRepositoryPort   messageRepository;
    private final VoiceTranscriptionPort  transcriptionPort;

    public TranscribeVoiceMessageUseCase(MessageRepositoryPort messageRepository,
                                          VoiceTranscriptionPort transcriptionPort) {
        this.messageRepository = Objects.requireNonNull(messageRepository);
        this.transcriptionPort = Objects.requireNonNull(transcriptionPort);
    }

    public TranscriptionResult execute(MessageId messageId) {
        Objects.requireNonNull(messageId, "messageId est obligatoire");

        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> ResourceNotFoundException.messageNotFound(messageId));

        if (!message.isVoice()) {
            throw new NotAVoiceMessageException("Le message " + messageId + " n'est pas un message vocal");
        }

        // Le contenu d'un VOICE message est l'URL audio
        String audioUrl = message.content().text();
        TranscriptionResult result = transcriptionPort.transcribe(messageId, audioUrl);

        message.enrich(MessageMetadata.withTranscription(result.text()));
        messageRepository.save(message);

        return result;
    }

    public static final class NotAVoiceMessageException extends RuntimeException {
        public NotAVoiceMessageException(String message) { super(message); }
    }
}
