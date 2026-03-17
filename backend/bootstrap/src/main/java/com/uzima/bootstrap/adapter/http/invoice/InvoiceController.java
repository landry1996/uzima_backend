package com.uzima.bootstrap.adapter.http.invoice;

import com.uzima.application.invoice.CancelInvoiceUseCase;
import com.uzima.application.invoice.CreateInvoiceUseCase;
import com.uzima.application.invoice.GetInvoiceUseCase;
import com.uzima.application.invoice.MarkInvoicePaidUseCase;
import com.uzima.application.invoice.SendInvoiceUseCase;
import com.uzima.application.invoice.port.in.CancelInvoiceCommand;
import com.uzima.application.invoice.port.in.CreateInvoiceCommand;
import com.uzima.application.invoice.port.in.MarkInvoicePaidCommand;
import com.uzima.application.invoice.port.in.SendInvoiceCommand;
import com.uzima.domain.invoice.model.Invoice;
import com.uzima.domain.invoice.model.InvoiceId;
import com.uzima.domain.invoice.model.InvoiceItem;
import com.uzima.domain.invoice.model.TaxRate;
import com.uzima.bootstrap.adapter.http.security.SecurityContextHelper;
import com.uzima.domain.payment.model.Currency;
import com.uzima.domain.user.model.UserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Adaptateur HTTP entrant : Facturation.
 * <p>
 * L'identité de l'utilisateur est extraite du JWT via {@link SecurityContextHelper}.
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final CreateInvoiceUseCase   createInvoiceUseCase;
    private final SendInvoiceUseCase     sendInvoiceUseCase;
    private final MarkInvoicePaidUseCase markInvoicePaidUseCase;
    private final CancelInvoiceUseCase   cancelInvoiceUseCase;
    private final GetInvoiceUseCase      getInvoiceUseCase;

    public InvoiceController(
            CreateInvoiceUseCase   createInvoiceUseCase,
            SendInvoiceUseCase     sendInvoiceUseCase,
            MarkInvoicePaidUseCase markInvoicePaidUseCase,
            CancelInvoiceUseCase   cancelInvoiceUseCase,
            GetInvoiceUseCase      getInvoiceUseCase
    ) {
        this.createInvoiceUseCase   = Objects.requireNonNull(createInvoiceUseCase);
        this.sendInvoiceUseCase     = Objects.requireNonNull(sendInvoiceUseCase);
        this.markInvoicePaidUseCase = Objects.requireNonNull(markInvoicePaidUseCase);
        this.cancelInvoiceUseCase   = Objects.requireNonNull(cancelInvoiceUseCase);
        this.getInvoiceUseCase      = Objects.requireNonNull(getInvoiceUseCase);
    }

    /**
     * POST /api/invoices
     * Crée une facture avec ses lignes.
     * 201 Created + invoiceId
     */
    @PostMapping
    public ResponseEntity<InvoiceIdResponse> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request
    ) {
        UserId issuerId = SecurityContextHelper.currentUserId();
        List<CreateInvoiceCommand.Item> items = request.items().stream()
                .map(i -> new CreateInvoiceCommand.Item(
                    i.description(),
                    i.quantity(),
                    i.unitAmount(),
                    Currency.valueOf(i.currency().toUpperCase()),
                    TaxRate.forCode(i.taxRateCode())
                ))
                .toList();

        var command = new CreateInvoiceCommand(
            issuerId,
            UserId.of(request.clientId()),
            request.dueDate(),
            items
        );
        var invoiceId = createInvoiceUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(new InvoiceIdResponse(invoiceId.toString()));
    }

    /**
     * GET /api/invoices/{id}
     * Récupère une facture par identifiant.
     * 200 OK + InvoiceResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable String id) {
        UserId requesterId = SecurityContextHelper.currentUserId();
        var invoice = getInvoiceUseCase.findById(InvoiceId.of(id), requesterId);
        return ResponseEntity.ok(InvoiceResponse.from(invoice));
    }

    /**
     * GET /api/invoices/sent
     * Factures émises par l'utilisateur courant.
     * 200 OK + List<InvoiceResponse>
     */
    @GetMapping("/sent")
    public ResponseEntity<List<InvoiceResponse>> getSentInvoices() {
        UserId userId = SecurityContextHelper.currentUserId();
        var invoices = getInvoiceUseCase.findSent(userId);
        return ResponseEntity.ok(invoices.stream().map(InvoiceResponse::from).toList());
    }

    /**
     * GET /api/invoices/received
     * Factures reçues par l'utilisateur courant.
     * 200 OK + List<InvoiceResponse>
     */
    @GetMapping("/received")
    public ResponseEntity<List<InvoiceResponse>> getReceivedInvoices() {
        UserId userId = SecurityContextHelper.currentUserId();
        var invoices = getInvoiceUseCase.findReceived(userId);
        return ResponseEntity.ok(invoices.stream().map(InvoiceResponse::from).toList());
    }

    /**
     * POST /api/invoices/{id}/send
     * Envoie la facture (DRAFT → SENT). Émetteur uniquement.
     * 204 No Content
     */
    @PostMapping("/{id}/send")
    public ResponseEntity<Void> sendInvoice(@PathVariable String id) {
        UserId requesterId = SecurityContextHelper.currentUserId();
        sendInvoiceUseCase.execute(new SendInvoiceCommand(InvoiceId.of(id), requesterId));
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/invoices/{id}/mark-paid
     * Marque la facture comme payée (SENT → PAID). Émetteur uniquement.
     * 204 No Content
     */
    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<Void> markAsPaid(@PathVariable String id) {
        UserId requesterId = SecurityContextHelper.currentUserId();
        markInvoicePaidUseCase.execute(new MarkInvoicePaidCommand(InvoiceId.of(id), requesterId));
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/invoices/{id}/cancel
     * Annule la facture (DRAFT/SENT/OVERDUE → CANCELLED). Émetteur uniquement.
     * 204 No Content
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelInvoice(@PathVariable String id) {
        UserId requesterId = SecurityContextHelper.currentUserId();
        cancelInvoiceUseCase.execute(new CancelInvoiceCommand(InvoiceId.of(id), requesterId));
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Request DTOs
    // -------------------------------------------------------------------------

    public record CreateInvoiceRequest(
            @NotBlank String clientId,
            @NotNull LocalDate dueDate,
            @NotEmpty List<InvoiceItemRequest> items
    ) {}

    public record InvoiceItemRequest(
            @NotBlank @Size(max = 500) String description,
            @Min(1) int quantity,
            @NotNull BigDecimal unitAmount,
            @NotBlank String currency,
            @NotBlank String taxRateCode
    ) {}

    // -------------------------------------------------------------------------
    // Response DTOs
    // -------------------------------------------------------------------------

    public record InvoiceIdResponse(String invoiceId) {}

    public record InvoiceResponse(
            String      invoiceId,
            String      issuerId,
            String      clientId,
            String      dueDate,
            String      status,
            String      createdAt,
            String      sentAt,
            String      paidAt,
            String      cancelledAt,
            BigDecimal  subtotal,
            BigDecimal  taxAmount,
            BigDecimal  total,
            String      currency,
            int         itemCount,
            List<InvoiceItemResponse> items
    ) {
        public static InvoiceResponse from(Invoice invoice) {
            String currency = invoice.items().isEmpty() ? "XOF"
                    : invoice.items().getFirst().unitPrice().currency().name();
            return new InvoiceResponse(
                invoice.id().toString(),
                invoice.issuerId().toString(),
                invoice.clientId().toString(),
                invoice.dueDate().toString(),
                invoice.status().displayName(),
                invoice.createdAt().toString(),
                invoice.sentAt().map(Object::toString).orElse(null),
                invoice.paidAt().map(Object::toString).orElse(null),
                invoice.cancelledAt().map(Object::toString).orElse(null),
                invoice.subtotal().amount(),
                invoice.taxAmount().amount(),
                invoice.total().amount(),
                currency,
                invoice.itemCount(),
                invoice.items().stream().map(InvoiceItemResponse::from).toList()
            );
        }
    }

    public record InvoiceItemResponse(
            String     itemId,
            String     description,
            int        quantity,
            BigDecimal unitAmount,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal total,
            String     taxRateLabel
    ) {
        public static InvoiceItemResponse from(InvoiceItem item) {
            return new InvoiceItemResponse(
                item.id().toString(),
                item.description(),
                item.quantity(),
                item.unitPrice().amount(),
                item.subtotal().amount(),
                item.taxAmount().amount(),
                item.total().amount(),
                item.taxRate().label()
            );
        }
    }
}
