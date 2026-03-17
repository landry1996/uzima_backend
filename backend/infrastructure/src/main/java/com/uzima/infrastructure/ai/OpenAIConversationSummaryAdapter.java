package com.uzima.infrastructure.ai;

import com.uzima.application.message.port.out.ConversationSummaryPort;
import com.uzima.infrastructure.shared.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Adaptateur production : Résumé de conversation via OpenAI Chat API (GPT-4o-mini).
 * <p>
 * Les messages sont concaténés et envoyés au modèle avec une instruction
 * de produire un résumé concis dans la langue spécifiée.
 */
public class OpenAIConversationSummaryAdapter implements ConversationSummaryPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAIConversationSummaryAdapter.class);
    private static final String MODEL = "gpt-4o-mini";
    private static final int MAX_MESSAGES = 50;

    private final RestClient restClient;

    public OpenAIConversationSummaryAdapter(String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public String summarize(List<String> messages, String language) {
        if (messages == null || messages.isEmpty()) return "";

        String systemPrompt = "You are a concise conversation summarizer. "
            + "Summarize the following conversation in " + language + ". "
            + "Keep it brief (2-3 sentences max). Return only the summary.";

        List<String> truncated = messages.size() > MAX_MESSAGES
            ? messages.subList(messages.size() - MAX_MESSAGES, messages.size())
            : messages;

        String conversationText = String.join("\n", truncated);

        try {
            ChatRequest request = new ChatRequest(MODEL, List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", conversationText)
            ));

            ChatResponse response = restClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                log.warn("[SUMMARY] Réponse OpenAI vide pour {} messages", messages.size());
                return "";
            }

            return response.choices().getFirst().message().content().trim();

        } catch (RestClientException ex) {
            throw new ExternalServiceException("OpenAI", "Échec du résumé de conversation", ex);
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
