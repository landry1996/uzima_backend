package com.uzima.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.uzima.application.message.port.out.IntentDetectionPort;
import com.uzima.infrastructure.shared.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Adaptateur production : Détection d'intention via OpenAI Chat API (GPT-4o-mini).
 * <p>
 * Envoie le texte à OpenAI avec un prompt de classification et parse
 * la réponse JSON {intent, confidence}.
 */
public class OpenAIIntentDetectionAdapter implements IntentDetectionPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAIIntentDetectionAdapter.class);
    private static final String MODEL = "gpt-4o-mini";
    private static final String SYSTEM_PROMPT = """
            You are an intent classifier for a social super-app.
            Classify the user message into exactly one of these intents:
            payment_request, meeting_scheduling, task_assignment, emergency, greeting, unknown.
            Respond with valid JSON only: {"intent": "<intent>", "confidence": <0.0-1.0>}
            """;

    private final RestClient restClient;

    public OpenAIIntentDetectionAdapter(String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public DetectedIntent detect(String text) {
        if (text == null || text.isBlank()) return DetectedIntent.unknown();

        try {
            ChatRequest request = new ChatRequest(MODEL, List.of(
                new ChatMessage("system", SYSTEM_PROMPT),
                new ChatMessage("user", text)
            ));

            ChatResponse response = restClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                log.warn("[INTENT] Réponse OpenAI vide pour le texte : {}", text);
                return DetectedIntent.unknown();
            }

            String content = response.choices().get(0).message().content();
            IntentApiResponse parsed = parseIntentResponse(content);
            return DetectedIntent.of(normalizeIntent(parsed.intent()), parsed.confidence());

        } catch (RestClientException ex) {
            throw new ExternalServiceException("OpenAI", "Échec de la détection d'intention", ex);
        }
    }

    private IntentApiResponse parseIntentResponse(String content) {
        try {
            // Parse manuel minimal pour éviter une dépendance ObjectMapper explicite
            String intent = extractJsonString(content, "intent");
            double confidence = extractJsonDouble(content, "confidence");
            return new IntentApiResponse(intent, confidence);
        } catch (Exception ex) {
            log.warn("[INTENT] Impossible de parser la réponse OpenAI : {}", content);
            return new IntentApiResponse(DetectedIntent.UNKNOWN, 0.0);
        }
    }

    private String normalizeIntent(String intent) {
        if (intent == null) return DetectedIntent.UNKNOWN;
        return switch (intent.toLowerCase().trim()) {
            case "payment_request"    -> DetectedIntent.PAYMENT_REQUEST;
            case "meeting_scheduling" -> DetectedIntent.MEETING_SCHEDULING;
            case "task_assignment"    -> DetectedIntent.TASK_ASSIGNMENT;
            case "emergency"          -> DetectedIntent.EMERGENCY;
            case "greeting"           -> DetectedIntent.GREETING;
            default                   -> DetectedIntent.UNKNOWN;
        };
    }

    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int start = json.indexOf('"', idx + search.length() + 1);
        int end   = json.indexOf('"', start + 1);
        return start >= 0 && end > start ? json.substring(start + 1, end) : null;
    }

    private static double extractJsonDouble(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return 0.0;
        int colon = json.indexOf(':', idx + search.length());
        int start = colon + 1;
        int end   = json.indexOf(',', start);
        if (end < 0) end = json.indexOf('}', start);
        String num = json.substring(start, end).trim();
        return Double.parseDouble(num);
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

    record IntentApiResponse(String intent, double confidence) {}
}
