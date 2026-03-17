package com.uzima.application.message;

import com.uzima.application.message.port.out.EmotionAnalysisPort;
import com.uzima.application.message.port.out.EmotionAnalysisPort.EmotionResult;
import com.uzima.application.message.port.out.MessageRepositoryPort;
import com.uzima.application.shared.exception.ResourceNotFoundException;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.message.model.MessageId;
import com.uzima.domain.message.model.MessageMetadata;

import java.util.Objects;

/** Use Case : Analyser l'émotion dans un message vocal. */
public class AnalyzeMessageEmotionUseCase {

    private final MessageRepositoryPort messageRepository;
    private final EmotionAnalysisPort   emotionAnalysisPort;

    public AnalyzeMessageEmotionUseCase(MessageRepositoryPort messageRepository,
                                         EmotionAnalysisPort emotionAnalysisPort) {
        this.messageRepository   = Objects.requireNonNull(messageRepository);
        this.emotionAnalysisPort = Objects.requireNonNull(emotionAnalysisPort);
    }

    public EmotionResult execute(MessageId messageId) {
        Objects.requireNonNull(messageId, "messageId est obligatoire");

        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> ResourceNotFoundException.messageNotFound(messageId));

        if (!message.isVoice()) {
            throw new TranscribeVoiceMessageUseCase.NotAVoiceMessageException(
                "L'analyse émotionnelle nécessite un message vocal : " + messageId
            );
        }

        String audioUrl = message.content().text();
        EmotionResult result = emotionAnalysisPort.analyze(audioUrl);

        message.enrich(MessageMetadata.withEmotion(result.primaryEmotion()));
        messageRepository.save(message);

        return result;
    }
}
