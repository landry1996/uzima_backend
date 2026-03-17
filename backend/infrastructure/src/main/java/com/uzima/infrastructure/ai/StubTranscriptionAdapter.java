package com.uzima.infrastructure.ai;

import com.uzima.application.message.port.out.VoiceTranscriptionPort;
import com.uzima.domain.message.model.MessageId;

/**
 * Adaptateur stub : Transcription vocale no-op (pour dev/test).
 * <p>
 * Remplacer par OpenAITranscriptionAdapter en production.
 */
public class StubTranscriptionAdapter implements VoiceTranscriptionPort {

    @Override
    public TranscriptionResult transcribe(MessageId messageId, String audioUrl) {
        // Stub : retourne une transcription simulée
        return TranscriptionResult.of(
            "[Transcription simulée pour " + messageId + "]", "fr", 0.0
        );
    }
}
