package com.uzima.application.message.port.out;

import com.uzima.domain.message.model.MessageId;

/** Port OUT : Transcription vocale → texte (ex. OpenAI Whisper). */
public interface VoiceTranscriptionPort {

    /**
     * Transcrit le contenu audio d'un message vocal.
     *
     * @param messageId Identifiant du message (pour traçabilité)
     * @param audioUrl  URL de l'audio à transcrire
     * @return Résultat de la transcription
     */
    TranscriptionResult transcribe(MessageId messageId, String audioUrl);

    record TranscriptionResult(String text, String detectedLanguage, double confidence) {

        public static TranscriptionResult of(String text, String language, double confidence) {
            return new TranscriptionResult(text, language, confidence);
        }
    }
}
