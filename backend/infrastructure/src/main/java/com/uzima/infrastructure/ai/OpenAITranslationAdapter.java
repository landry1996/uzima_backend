package com.uzima.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.uzima.application.message.port.out.TranslationPort;
import com.uzima.infrastructure.shared.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Adaptateur production : Traduction via OpenAI Chat API (GPT-4o-mini).
 * <p>
 * Le modèle est instruit de retourner uniquement le texte traduit,
 * sans explication ni préfixe.
 */
public class OpenAITranslationAdapter implements TranslationPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAITranslationAdapter.class);
    private static final String MODEL = "gpt-4o-mini";

    private final RestClient restClient;

    public OpenAITranslationAdapter(String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public String translate(String text, String targetLanguage) {
        if (text == null || text.isBlank()) return text;

        String systemPrompt = "You are a professional translator. "
            + "Translate the user's message to " + targetLanguage + ". "
            + "Return only the translated text with no explanation.";

        try {
            ChatRequest request = new ChatRequest(MODEL, List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", text)
            ));

            ChatResponse response = restClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                log.warn("[TRANSLATION] Réponse OpenAI vide — retour du texte original");
                return text;
            }

            return response.choices().getFirst().message().content().trim();

        } catch (RestClientException ex) {
            throw new ExternalServiceException("OpenAI", "Échec de la traduction", ex);
        }
    }

    // -------------------------------------------------------------------------
    // DTOs internes (OpenAI API)
    // -------------------------------------------------------------------------

    record ChatRequest(String model, List<ChatMessage> messages) {}

    record ChatMessage(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<ChatChoice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatChoice(ChatMessage message) {}
}
