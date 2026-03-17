package com.uzima.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.uzima.application.message.port.out.VoiceTranscriptionPort;
import com.uzima.domain.message.model.MessageId;
import com.uzima.infrastructure.shared.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur production : Transcription vocale via OpenAI Whisper API.
 * <p>
 * Flux :
 * 1. Téléchargement de l'audio depuis l'URL fournie
 * 2. Envoi en multipart à /v1/audio/transcriptions (modèle whisper-1)
 * 3. Extraction du texte transcrit et de la langue détectée
 */
public class OpenAITranscriptionAdapter implements VoiceTranscriptionPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAITranscriptionAdapter.class);
    private static final String WHISPER_MODEL = "whisper-1";

    private final RestClient openAiClient;
    private final RestClient downloadClient;

    public OpenAITranscriptionAdapter(String apiKey) {
        this.openAiClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.downloadClient = RestClient.builder().build();
    }

    @Override
    public TranscriptionResult transcribe(MessageId messageId, String audioUrl) {
        if (audioUrl == null || audioUrl.isBlank()) {
            return TranscriptionResult.of("", "unknown", 0.0);
        }

        try {
            // 1. Télécharger l'audio depuis l'URL
            byte[] audioBytes = downloadClient.get()
                    .uri(audioUrl)
                    .retrieve()
                    .body(byte[].class);

            if (audioBytes == null || audioBytes.length == 0) {
                log.warn("[TRANSCRIPTION] Audio vide pour le message {}", messageId);
                return TranscriptionResult.of("", "unknown", 0.0);
            }

            // 2. Envoyer à Whisper en multipart
            String filename = extractFilename(audioUrl);
            ByteArrayResource audioResource = new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() { return filename; }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("model", WHISPER_MODEL);
            body.add("file", audioResource);
            body.add("response_format", "verbose_json");

            WhisperResponse response = openAiClient.post()
                    .uri("/v1/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(WhisperResponse.class);

            if (response == null || response.text() == null) {
                log.warn("[TRANSCRIPTION] Réponse Whisper vide pour le message {}", messageId);
                return TranscriptionResult.of("", "unknown", 0.0);
            }

            String language = response.language() != null ? response.language() : "unknown";
            log.debug("[TRANSCRIPTION] Message {} transcrit ({} mots)", messageId,
                      response.text().split("\\s+").length);
            return TranscriptionResult.of(response.text().trim(), language, 1.0);

        } catch (RestClientException ex) {
            throw new ExternalServiceException(
                "OpenAI Whisper", "Échec de la transcription du message " + messageId, ex
            );
        }
    }

    private static String extractFilename(String audioUrl) {
        int lastSlash = audioUrl.lastIndexOf('/');
        String name = lastSlash >= 0 ? audioUrl.substring(lastSlash + 1) : "audio";
        // Supprime les query params si présents
        int queryIdx = name.indexOf('?');
        name = queryIdx >= 0 ? name.substring(0, queryIdx) : name;
        // Garde l'extension si reconnue par Whisper
        if (!name.matches(".*\\.(mp3|mp4|mpeg|mpga|m4a|wav|webm|ogg)$")) {
            name = name + ".mp3";
        }
        return name;
    }

    // -------------------------------------------------------------------------
    // DTO interne (Whisper API verbose_json)
    // -------------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WhisperResponse(String text, String language, Double duration) {}
}
