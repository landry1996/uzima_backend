package com.uzima.domain.message;

import com.uzima.domain.message.model.*;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Message — Enrichissement IA (MessageMetadata)")
class MessageEnrichmentTest {

    private static final Instant NOW = Instant.parse("2026-03-13T10:00:00Z");
    private final TimeProvider clock = () -> NOW;

    private Message textMessage;
    private Message voiceMessage;

    @BeforeEach
    void setUp() {
        UserId sender         = UserId.generate();
        ConversationId convId = ConversationId.generate();

        textMessage = Message.sendText(convId, sender, MessageContent.of("Bonjour !"), clock);

        voiceMessage = Message.reconstitute(
            MessageId.generate(), convId, sender,
            MessageContent.of("https://storage.example.com/audio/123.ogg"),
            Message.MessageType.VOICE, NOW, false, null, null
        );
    }

    // =========================================================================
    @Nested
    @DisplayName("metadata() initial")
    class InitialStateTest {

        @Test
        @DisplayName("un nouveau message n'a pas de métadonnées")
        void new_message_has_no_metadata() {
            assertThat(textMessage.metadata()).isEmpty();
        }

        @Test
        @DisplayName("isVoice() retourne true pour un message VOICE")
        void is_voice_true_for_voice_message() {
            assertThat(voiceMessage.isVoice()).isTrue();
            assertThat(textMessage.isVoice()).isFalse();
        }

        @Test
        @DisplayName("hasTranscription() retourne false sans transcription")
        void has_transcription_false_initially() {
            assertThat(voiceMessage.hasTranscription()).isFalse();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("enrich()")
    class EnrichTest {

        @Test
        @DisplayName("ajoute une transcription")
        void adds_transcription() {
            textMessage.enrich(MessageMetadata.withTranscription("Bonjour tout le monde"));
            assertThat(textMessage.metadata()).isPresent();
            assertThat(textMessage.metadata().get().transcription())
                .contains("Bonjour tout le monde");
        }

        @Test
        @DisplayName("ajoute une traduction")
        void adds_translation() {
            textMessage.enrich(MessageMetadata.withTranslation("Hello!", "en"));
            assertThat(textMessage.metadata().get().translation()).contains("Hello!");
            assertThat(textMessage.metadata().get().targetLanguage()).contains("en");
        }

        @Test
        @DisplayName("ajoute une intention détectée")
        void adds_intent() {
            textMessage.enrich(MessageMetadata.withIntent("payment_request"));
            assertThat(textMessage.metadata().get().detectedIntent()).contains("payment_request");
        }

        @Test
        @DisplayName("ajoute une émotion détectée")
        void adds_emotion() {
            voiceMessage.enrich(MessageMetadata.withEmotion("joy"));
            assertThat(voiceMessage.metadata().get().detectedEmotion()).contains("joy");
            assertThat(voiceMessage.hasTranscription()).isFalse();
        }

        @Test
        @DisplayName("fusionne les enrichissements successifs")
        void merges_successive_enrichments() {
            textMessage.enrich(MessageMetadata.withTranscription("Texte original"));
            textMessage.enrich(MessageMetadata.withTranslation("Original text", "en"));

            MessageMetadata meta = textMessage.metadata().get();
            assertThat(meta.transcription()).contains("Texte original");
            assertThat(meta.translation()).contains("Original text");
        }

        @Test
        @DisplayName("l'enrichissement ultérieur ne remplace pas les champs déjà définis")
        void later_enrich_does_not_overwrite_existing() {
            textMessage.enrich(MessageMetadata.withIntent("greeting"));
            // Deuxième enrich avec intent différent — le premier gagne dans mergeWith
            textMessage.enrich(MessageMetadata.withIntent("unknown"));

            // "greeting" doit rester (mergeWith: this wins)
            assertThat(textMessage.metadata().get().detectedIntent()).contains("greeting");
        }

        @Test
        @DisplayName("rejette un null")
        void rejects_null_metadata() {
            assertThatThrownBy(() -> textMessage.enrich(null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("hasTranscription() retourne true après transcription")
        void has_transcription_true_after_enrich() {
            voiceMessage.enrich(MessageMetadata.withTranscription("Salut"));
            assertThat(voiceMessage.hasTranscription()).isTrue();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("MessageMetadata.mergeWith()")
    class MergeTest {

        @Test
        @DisplayName("merge with null retourne this")
        void merge_with_null_returns_self() {
            MessageMetadata meta = MessageMetadata.withIntent("greeting");
            assertThat(meta.mergeWith(null)).isSameAs(meta);
        }

        @Test
        @DisplayName("champs null de this sont remplis par other")
        void fills_null_fields_from_other() {
            MessageMetadata a = MessageMetadata.withTranscription("Texte");
            MessageMetadata b = MessageMetadata.withIntent("greeting");

            MessageMetadata merged = a.mergeWith(b);
            assertThat(merged.transcription()).contains("Texte");
            assertThat(merged.detectedIntent()).contains("greeting");
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("reconstitute() avec metadata")
    class ReconstituteTest {

        @Test
        @DisplayName("reconstitue avec métadonnées préexistantes")
        void reconstitutes_with_metadata() {
            MessageMetadata meta = new MessageMetadata("Transcription", "Translation", "en", "greeting", "joy");
            Message r = Message.reconstitute(
                MessageId.generate(),
                ConversationId.generate(),
                UserId.generate(),
                MessageContent.of("audio-url"),
                Message.MessageType.VOICE,
                NOW, false, null, meta
            );

            assertThat(r.metadata()).isPresent();
            assertThat(r.metadata().get().transcription()).contains("Transcription");
            assertThat(r.metadata().get().detectedEmotion()).contains("joy");
            assertThat(r.hasTranscription()).isTrue();
        }

        @Test
        @DisplayName("reconstitue sans métadonnées (compatibilité)")
        void reconstitutes_without_metadata_backward_compatible() {
            Message r = Message.reconstitute(
                MessageId.generate(), ConversationId.generate(), UserId.generate(),
                MessageContent.of("Hello"), Message.MessageType.TEXT,
                NOW, false, null
            );
            assertThat(r.metadata()).isEmpty();
        }
    }
}
