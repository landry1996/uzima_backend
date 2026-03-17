package com.uzima.bootstrap.adapter.http;

import com.uzima.application.message.*;
import com.uzima.application.message.port.in.GetConversationQuery;
import com.uzima.application.message.port.in.StartConversationCommand;
import com.uzima.application.message.port.out.IntentDetectionPort.DetectedIntent;
import com.uzima.application.message.port.out.VoiceTranscriptionPort.TranscriptionResult;
import com.uzima.bootstrap.adapter.http.dto.SendMessageRequest;
import com.uzima.bootstrap.adapter.http.mapper.MessageHttpMapper;
import com.uzima.bootstrap.adapter.http.mapper.MessageHttpMapper.MessageResponse;
import com.uzima.bootstrap.adapter.http.security.SecurityContextHelper;
import com.uzima.domain.message.model.Conversation;
import com.uzima.domain.message.model.ConversationId;
import com.uzima.domain.message.model.Message;
import com.uzima.domain.message.model.MessageId;
import com.uzima.domain.user.model.UserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adaptateur HTTP entrant : Messagerie et Conversations.
 * <p>
 * Traduit les requêtes HTTP en commandes/requêtes applicatives.
 * L'identité de l'utilisateur est extraite du JWT via {@link SecurityContextHelper}.
 */
@RestController
@RequestMapping("/api")
public class MessageController {

    private final StartConversationUseCase        startConversationUseCase;
    private final GetUserConversationsUseCase      getUserConversationsUseCase;
    private final SendMessageUseCase              sendMessageUseCase;
    private final GetConversationUseCase          getConversationUseCase;
    private final TranscribeVoiceMessageUseCase   transcribeUseCase;
    private final TranslateMessageUseCase         translateUseCase;
    private final DetectMessageIntentUseCase      detectIntentUseCase;
    private final AnalyzeMessageEmotionUseCase    analyzeEmotionUseCase;
    private final SummarizeConversationUseCase    summarizeUseCase;
    private final SearchMessagesByIntentUseCase   searchByIntentUseCase;

    public MessageController(
            StartConversationUseCase startConversationUseCase,
            GetUserConversationsUseCase getUserConversationsUseCase,
            SendMessageUseCase sendMessageUseCase,
            GetConversationUseCase getConversationUseCase,
            TranscribeVoiceMessageUseCase transcribeUseCase,
            TranslateMessageUseCase translateUseCase,
            DetectMessageIntentUseCase detectIntentUseCase,
            AnalyzeMessageEmotionUseCase analyzeEmotionUseCase,
            SummarizeConversationUseCase summarizeUseCase,
            SearchMessagesByIntentUseCase searchByIntentUseCase
    ) {
        this.startConversationUseCase   = Objects.requireNonNull(startConversationUseCase);
        this.getUserConversationsUseCase= Objects.requireNonNull(getUserConversationsUseCase);
        this.sendMessageUseCase         = Objects.requireNonNull(sendMessageUseCase);
        this.getConversationUseCase     = Objects.requireNonNull(getConversationUseCase);
        this.transcribeUseCase          = Objects.requireNonNull(transcribeUseCase);
        this.translateUseCase           = Objects.requireNonNull(translateUseCase);
        this.detectIntentUseCase        = Objects.requireNonNull(detectIntentUseCase);
        this.analyzeEmotionUseCase      = Objects.requireNonNull(analyzeEmotionUseCase);
        this.summarizeUseCase           = Objects.requireNonNull(summarizeUseCase);
        this.searchByIntentUseCase      = Objects.requireNonNull(searchByIntentUseCase);
    }

    // =========================================================================
    // Conversations
    // =========================================================================

    /**
     * POST /api/conversations
     * Démarre ou retrouve une conversation directe avec un autre utilisateur.
     * Idempotent : si une conversation directe existe déjà, la retourne.
     * 200 OK + ConversationResponse
     */
    @PostMapping("/conversations")
    public ResponseEntity<ConversationResponse> startConversation(
            @Valid @RequestBody StartConversationRequest request
    ) {
        UserId currentUser = SecurityContextHelper.currentUserId();
        var command = new StartConversationCommand(currentUser, UserId.of(request.targetUserId()));
        Conversation conversation = startConversationUseCase.execute(command);
        return ResponseEntity.ok(ConversationResponse.from(conversation));
    }

    /**
     * GET /api/conversations
     * Liste toutes les conversations de l'utilisateur authentifié.
     * 200 OK + List&lt;ConversationResponse&gt;
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> listConversations() {
        UserId currentUser = SecurityContextHelper.currentUserId();
        List<Conversation> conversations = getUserConversationsUseCase.execute(currentUser);
        return ResponseEntity.ok(
            conversations.stream().map(ConversationResponse::from).toList()
        );
    }

    // =========================================================================
    // Messages
    // =========================================================================

    /**
     * POST /api/conversations/{conversationId}/messages
     * Envoie un message dans une conversation.
     */
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable String conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        UserId currentUser = SecurityContextHelper.currentUserId();
        var command = MessageHttpMapper.toSendCommand(conversationId, currentUser, request);
        var message = sendMessageUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(MessageHttpMapper.toResponse(message));
    }

    /**
     * GET /api/conversations/{conversationId}/messages
     * Récupère les messages d'une conversation (paginés).
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0")  int offset
    ) {
        UserId currentUser = SecurityContextHelper.currentUserId();
        var query = new GetConversationQuery(
                ConversationId.of(conversationId),
                currentUser,
                limit,
                offset
        );
        var view = getConversationUseCase.execute(query);
        return ResponseEntity.ok(MessageHttpMapper.toResponseList(view.messages()));
    }

    // =========================================================================
    // Analyse IA des messages
    // =========================================================================

    @PostMapping("/messages/{id}/transcribe")
    public ResponseEntity<Map<String, Object>> transcribe(@PathVariable UUID id) {
        TranscriptionResult result = transcribeUseCase.execute(MessageId.of(id));
        return ResponseEntity.ok(Map.of(
            "text",             result.text(),
            "detectedLanguage", result.detectedLanguage(),
            "confidence",       result.confidence()
        ));
    }

    @PostMapping("/messages/{id}/translate")
    public ResponseEntity<Map<String, String>> translate(
            @PathVariable UUID id,
            @RequestParam String targetLanguage
    ) {
        String translated = translateUseCase.execute(MessageId.of(id), targetLanguage);
        return ResponseEntity.ok(Map.of("translation", translated, "targetLanguage", targetLanguage));
    }

    @PostMapping("/messages/{id}/detect-intent")
    public ResponseEntity<Map<String, Object>> detectIntent(@PathVariable UUID id) {
        DetectedIntent result = detectIntentUseCase.execute(MessageId.of(id));
        return ResponseEntity.ok(Map.of(
            "intent",     result.intent(),
            "confidence", result.confidence()
        ));
    }

    @PostMapping("/messages/{id}/analyze-emotion")
    public ResponseEntity<Map<String, Object>> analyzeEmotion(@PathVariable UUID id) {
        var result = analyzeEmotionUseCase.execute(MessageId.of(id));
        return ResponseEntity.ok(Map.of(
            "emotion",    result.primaryEmotion(),
            "confidence", result.confidence()
        ));
    }

    @GetMapping("/conversations/{conversationId}/summary")
    public ResponseEntity<Map<String, String>> summarize(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "fr") String language
    ) {
        UserId currentUser = SecurityContextHelper.currentUserId();
        String summary = summarizeUseCase.execute(
            ConversationId.of(conversationId), currentUser, language
        );
        return ResponseEntity.ok(Map.of("summary", summary));
    }

    @GetMapping("/conversations/{conversationId}/messages/search")
    public ResponseEntity<List<MessageResponse>> searchByIntent(
            @PathVariable String conversationId,
            @RequestParam String intent
    ) {
        UserId currentUser = SecurityContextHelper.currentUserId();
        List<Message> messages = searchByIntentUseCase.execute(
            ConversationId.of(conversationId), currentUser, intent
        );
        return ResponseEntity.ok(MessageHttpMapper.toResponseList(messages));
    }

    // =========================================================================
    // DTOs
    // =========================================================================

    public record StartConversationRequest(
            @NotBlank(message = "L'identifiant du destinataire est obligatoire")
            String targetUserId
    ) {}

    public record ConversationResponse(
            String id,
            String type,
            String title,
            Set<String> participantIds,
            Instant createdAt
    ) {
        public static ConversationResponse from(Conversation c) {
            return new ConversationResponse(
                c.id().toString(),
                c.type().name(),
                c.title().orElse(null),
                c.participants().stream().map(UserId::toString).collect(Collectors.toSet()),
                c.createdAt()
            );
        }
    }

}
