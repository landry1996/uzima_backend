package com.uzima.domain.message.model;

import java.util.Optional;

/**
 * Value Object : Métadonnées IA enrichissant un message.
 * Immuable. Tous les champs sont optionnels — seuls les champs
 * réellement calculés sont présents.
 */
public record MessageMetadata(
        String rawTranscription,   // Transcription vocale → texte (Whisper)
        String rawTranslation,     // Traduction du contenu
        String rawTargetLanguage,  // Code langue de la traduction (ex. "fr", "en")
        String rawDetectedIntent,  // Intention détectée (ex. "payment_request", "meeting_scheduling")
        String rawDetectedEmotion  // Émotion détectée (ex. "joy", "stress", "neutral")
) {

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    public static MessageMetadata withTranscription(String transcription) {
        return new MessageMetadata(transcription, null, null, null, null);
    }

    public static MessageMetadata withTranslation(String translation, String targetLanguage) {
        return new MessageMetadata(null, translation, targetLanguage, null, null);
    }

    public static MessageMetadata withIntent(String detectedIntent) {
        return new MessageMetadata(null, null, null, detectedIntent, null);
    }

    public static MessageMetadata withEmotion(String detectedEmotion) {
        return new MessageMetadata(null, null, null, null, detectedEmotion);
    }

    // -------------------------------------------------------------------------
    // Accesseurs optionnels (noms sémantiques, sans le préfixe "raw")
    // -------------------------------------------------------------------------

    public Optional<String> transcription()   { return Optional.ofNullable(rawTranscription); }
    public Optional<String> translation()     { return Optional.ofNullable(rawTranslation); }
    public Optional<String> targetLanguage()  { return Optional.ofNullable(rawTargetLanguage); }
    public Optional<String> detectedIntent()  { return Optional.ofNullable(rawDetectedIntent); }
    public Optional<String> detectedEmotion() { return Optional.ofNullable(rawDetectedEmotion); }

    // -------------------------------------------------------------------------
    // Fusion : merge two metadata objects (this wins on non-null fields)
    // -------------------------------------------------------------------------

    public MessageMetadata mergeWith(MessageMetadata other) {
        if (other == null) return this;
        return new MessageMetadata(
            rawTranscription   != null ? rawTranscription   : other.rawTranscription,
            rawTranslation     != null ? rawTranslation     : other.rawTranslation,
            rawTargetLanguage  != null ? rawTargetLanguage  : other.rawTargetLanguage,
            rawDetectedIntent  != null ? rawDetectedIntent  : other.rawDetectedIntent,
            rawDetectedEmotion != null ? rawDetectedEmotion : other.rawDetectedEmotion
        );
    }
}
