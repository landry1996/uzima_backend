package com.uzima.bootstrap.adapter.http.payment;

import com.uzima.application.payment.CancelTransactionUseCase;
import com.uzima.application.payment.GetTransactionHistoryUseCase;
import com.uzima.application.payment.RequestPaymentUseCase;
import com.uzima.application.payment.SendPaymentUseCase;
import com.uzima.bootstrap.adapter.http.security.SecurityContextHelper;
import com.uzima.domain.payment.model.TransactionId;
import com.uzima.domain.user.model.UserId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * Adaptateur HTTP entrant : Paiements.
 * <p>
 * Responsabilités strictes :
 * - Lier les requêtes HTTP aux commandes applicatives
 * - Déléguer aux Use Cases via PaymentHttpMapper
 * - Retourner les réponses HTTP avec les bons codes statut
 * <p>
 * Pas de logique métier ici. Pas de @ExceptionHandler (centralisé dans GlobalExceptionHandler).
 * <p>
 * L'identité de l'utilisateur est extraite du JWT via {@link SecurityContextHelper}.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final SendPaymentUseCase sendPaymentUseCase;
    private final RequestPaymentUseCase requestPaymentUseCase;
    private final GetTransactionHistoryUseCase getTransactionHistoryUseCase;
    private final CancelTransactionUseCase cancelTransactionUseCase;

    public PaymentController(
            SendPaymentUseCase sendPaymentUseCase,
            RequestPaymentUseCase requestPaymentUseCase,
            GetTransactionHistoryUseCase getTransactionHistoryUseCase,
            CancelTransactionUseCase cancelTransactionUseCase
    ) {
        this.sendPaymentUseCase          = Objects.requireNonNull(sendPaymentUseCase);
        this.requestPaymentUseCase       = Objects.requireNonNull(requestPaymentUseCase);
        this.getTransactionHistoryUseCase = Objects.requireNonNull(getTransactionHistoryUseCase);
        this.cancelTransactionUseCase    = Objects.requireNonNull(cancelTransactionUseCase);
    }

    /**
     * POST /api/payments/send
     * Envoie un paiement.
     * 201 Created + transactionId
     */
    @PostMapping("/send")
    public ResponseEntity<TransactionIdResponse> sendPayment(
            @Valid @RequestBody SendPaymentRequest request
    ) {
        UserId senderId = SecurityContextHelper.currentUserId();
        var command = PaymentHttpMapper.toSendCommand(request, senderId);
        TransactionId id = sendPaymentUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TransactionIdResponse(id.toString()));
    }

    /**
     * POST /api/payments/request
     * Demande un paiement à un autre utilisateur.
     * 201 Created + transactionId
     */
    @PostMapping("/request")
    public ResponseEntity<TransactionIdResponse> requestPayment(
            @Valid @RequestBody RequestPaymentRequest request
    ) {
        UserId requesterId = SecurityContextHelper.currentUserId();
        var command = PaymentHttpMapper.toRequestCommand(request, requesterId);
        TransactionId id = requestPaymentUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TransactionIdResponse(id.toString()));
    }

    /**
     * GET /api/payments/history?limit=20&offset=0
     * Historique paginé des transactions de l'utilisateur courant.
     * 200 OK + TransactionHistoryResponse
     */
    @GetMapping("/history")
    public ResponseEntity<TransactionHistoryResponse> getHistory(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0")  int offset
    ) {
        UserId userId = SecurityContextHelper.currentUserId();
        var view = getTransactionHistoryUseCase.execute(userId, limit, offset);
        return ResponseEntity.ok(TransactionHistoryResponse.from(view));
    }

    /**
     * DELETE /api/payments/{id}
     * Annule une transaction PENDING.
     * 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable String id) {
        UserId userId = SecurityContextHelper.currentUserId();
        cancelTransactionUseCase.execute(TransactionId.of(id), userId);
        return ResponseEntity.noContent().build();
    }

    /** DTO de réponse minimal : l'identifiant de la transaction créée. */
    public record TransactionIdResponse(String transactionId) {}
}
