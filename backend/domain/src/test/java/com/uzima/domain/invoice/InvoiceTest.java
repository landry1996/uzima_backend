package com.uzima.domain.invoice;

import com.uzima.domain.invoice.model.Invoice;
import com.uzima.domain.invoice.model.InvoiceStatus;
import com.uzima.domain.invoice.model.TaxRate;
import com.uzima.domain.payment.model.Currency;
import com.uzima.domain.payment.model.Money;
import com.uzima.domain.shared.DomainEvent;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Invoice (Aggregate Root)")
class InvoiceTest {

    private static final Instant NOW   = Instant.parse("2026-03-13T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-03-13T11:00:00Z");

    private final TimeProvider clock      = () -> NOW;
    private final TimeProvider laterClock = () -> LATER;

    private UserId    issuer;
    private UserId    client;
    private LocalDate dueDate;

    @BeforeEach
    void setUp() {
        issuer  = UserId.generate();
        client  = UserId.generate();
        dueDate = LocalDate.of(2026, 4, 1);
    }

    @Nested
    @DisplayName("create()")
    class CreateTest {

        @Test
        @DisplayName("crée une facture en état DRAFT")
        void createsInvoiceAsDraft() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);

            assertThat(invoice.id()).isNotNull();
            assertThat(invoice.status()).isEqualTo(InvoiceStatus.DRAFT);
            assertThat(invoice.issuerId()).isEqualTo(issuer);
            assertThat(invoice.clientId()).isEqualTo(client);
            assertThat(invoice.dueDate()).isEqualTo(dueDate);
            assertThat(invoice.createdAt()).isEqualTo(NOW);
            assertThat(invoice.itemCount()).isZero();
        }

        @Test
        @DisplayName("lève SelfInvoicingException si émetteur == client")
        void throwsWhenSelfInvoicing() {
            assertThatThrownBy(() -> Invoice.create(issuer, issuer, dueDate, clock))
                    .isInstanceOf(Invoice.SelfInvoicingException.class);
        }
    }

    @Nested
    @DisplayName("addItem()")
    class AddItemTest {

        @Test
        @DisplayName("ajoute une ligne à une facture DRAFT")
        void addsItemToDraft() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);

            invoice.addItem("Prestation web", 2, Money.of(new BigDecimal("100.00"), Currency.XOF), TaxRate.TVA_18);

            assertThat(invoice.itemCount()).isEqualTo(1);
            assertThat(invoice.items().get(0).description()).isEqualTo("Prestation web");
            assertThat(invoice.items().get(0).quantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("lève InvoiceAlreadySentException si facture envoyée")
        void throwsWhenInvoiceSent() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);
            invoice.addItem("Ligne 1", 1, Money.of(new BigDecimal("500.00"), Currency.XOF), TaxRate.EXEMPT);
            invoice.send(clock);

            assertThatThrownBy(() ->
                invoice.addItem("Nouvelle ligne", 1, Money.of(new BigDecimal("100.00"), Currency.XOF), TaxRate.EXEMPT)
            ).isInstanceOf(Invoice.InvoiceAlreadySentException.class);
        }

        @Test
        @DisplayName("lève CurrencyMismatchException si devise différente")
        void throwsWhenCurrencyMismatch() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);
            invoice.addItem("Ligne XOF", 1, Money.of(new BigDecimal("100.00"), Currency.XOF), TaxRate.EXEMPT);

            assertThatThrownBy(() ->
                invoice.addItem("Ligne EUR", 1, Money.of(new BigDecimal("50.00"), Currency.EUR), TaxRate.EXEMPT)
            ).isInstanceOf(Invoice.CurrencyMismatchException.class);
        }
    }

    @Nested
    @DisplayName("Calculs financiers")
    class FinancialCalculationsTest {

        @Test
        @DisplayName("subtotal() = somme des quantity × unitPrice")
        void subtotalCalculation() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);
            invoice.addItem("Ligne A", 2, Money.of(new BigDecimal("100.00"), Currency.XOF), TaxRate.EXEMPT);
            invoice.addItem("Ligne B", 3, Money.of(new BigDecimal("50.00"),  Currency.XOF), TaxRate.EXEMPT);

            // 2×100 + 3×50 = 200 + 150 = 350
            assertThat(invoice.subtotal().amount()).isEqualByComparingTo(new BigDecimal("350.00"));
        }

        @Test
        @DisplayName("taxAmount() = somme des taxe par ligne")
        void taxAmountCalculation() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);
            // 1 × 1000 XOF à 18% = 180 XOF de taxe
            invoice.addItem("Prestation", 1, Money.of(new BigDecimal("1000.00"), Currency.XOF), TaxRate.TVA_18);

            assertThat(invoice.taxAmount().amount()).isEqualByComparingTo(new BigDecimal("180.00"));
        }

        @Test
        @DisplayName("total() = subtotal + taxAmount")
        void totalCalculation() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);
            // 1 × 1000 XOF à 18% → subtotal=1000, tax=180, total=1180
            invoice.addItem("Prestation", 1, Money.of(new BigDecimal("1000.00"), Currency.XOF), TaxRate.TVA_18);

            assertThat(invoice.total().amount()).isEqualByComparingTo(new BigDecimal("1180.00"));
        }

        @Test
        @DisplayName("subtotal() retourne zero si facture vide")
        void subtotalZeroWhenEmpty() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);

            assertThat(invoice.subtotal().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("send()")
    class SendTest {

        @Test
        @DisplayName("DRAFT → SENT avec sentAt renseigné")
        void sendsDraftInvoice() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);
            invoice.addItem("Prestation", 1, Money.of(new BigDecimal("500.00"), Currency.XOF), TaxRate.EXEMPT);

            invoice.send(laterClock);

            assertThat(invoice.status()).isEqualTo(InvoiceStatus.SENT);
            assertThat(invoice.sentAt()).contains(LATER);
        }

        @Test
        @DisplayName("émet InvoiceSentEvent")
        void emitsSentEvent() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);
            invoice.addItem("Prestation", 1, Money.of(new BigDecimal("500.00"), Currency.XOF), TaxRate.EXEMPT);

            invoice.send(clock);
            List<DomainEvent> events = invoice.pullDomainEvents();

            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(Invoice.InvoiceSentEvent.class);
            Invoice.InvoiceSentEvent event = (Invoice.InvoiceSentEvent) events.get(0);
            assertThat(event.issuerId()).isEqualTo(issuer);
            assertThat(event.clientId()).isEqualTo(client);
        }

        @Test
        @DisplayName("lève InvoiceCannotBeSentException si facture vide")
        void throwsWhenEmpty() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);

            assertThatThrownBy(() -> invoice.send(clock))
                    .isInstanceOf(Invoice.InvoiceCannotBeSentException.class);
        }

        @Test
        @DisplayName("lève IllegalTransitionException si déjà envoyée")
        void throwsWhenAlreadySent() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);
            invoice.addItem("Ligne", 1, Money.of(new BigDecimal("100.00"), Currency.XOF), TaxRate.EXEMPT);
            invoice.send(clock);

            assertThatThrownBy(() -> invoice.send(laterClock))
                    .isInstanceOf(Invoice.IllegalTransitionException.class);
        }
    }

    @Nested
    @DisplayName("markAsPaid()")
    class MarkAsPaidTest {

        @Test
        @DisplayName("SENT → PAID avec paidAt renseigné")
        void marksAsPaid() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);
            invoice.addItem("Prestation", 1, Money.of(new BigDecimal("500.00"), Currency.XOF), TaxRate.EXEMPT);
            invoice.send(clock);

            invoice.markAsPaid(laterClock);

            assertThat(invoice.status()).isEqualTo(InvoiceStatus.PAID);
            assertThat(invoice.paidAt()).contains(LATER);
        }

        @Test
        @DisplayName("émet InvoicePaidEvent")
        void emitsPaidEvent() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);
            invoice.addItem("Prestation", 1, Money.of(new BigDecimal("500.00"), Currency.XOF), TaxRate.EXEMPT);
            invoice.send(clock);
            invoice.pullDomainEvents();

            invoice.markAsPaid(clock);
            List<DomainEvent> events = invoice.pullDomainEvents();

            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(Invoice.InvoicePaidEvent.class);
        }

        @Test
        @DisplayName("lève IllegalTransitionException si DRAFT")
        void throwsWhenDraft() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);

            assertThatThrownBy(() -> invoice.markAsPaid(clock))
                    .isInstanceOf(Invoice.IllegalTransitionException.class);
        }
    }

    @Nested
    @DisplayName("cancel()")
    class CancelTest {

        @Test
        @DisplayName("annule une facture DRAFT")
        void cancelsDraft() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);

            invoice.cancel(laterClock);

            assertThat(invoice.status()).isEqualTo(InvoiceStatus.CANCELLED);
            assertThat(invoice.cancelledAt()).contains(LATER);
        }

        @Test
        @DisplayName("annule une facture SENT")
        void cancelsSent() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);
            invoice.addItem("Ligne", 1, Money.of(new BigDecimal("100.00"), Currency.XOF), TaxRate.EXEMPT);
            invoice.send(clock);

            invoice.cancel(laterClock);

            assertThat(invoice.status()).isEqualTo(InvoiceStatus.CANCELLED);
        }

        @Test
        @DisplayName("lève IllegalTransitionException si PAID")
        void throwsWhenPaid() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);
            invoice.addItem("Ligne", 1, Money.of(new BigDecimal("100.00"), Currency.XOF), TaxRate.EXEMPT);
            invoice.send(clock);
            invoice.markAsPaid(clock);

            assertThatThrownBy(() -> invoice.cancel(laterClock))
                    .isInstanceOf(Invoice.IllegalTransitionException.class);
        }

        @Test
        @DisplayName("lève IllegalTransitionException si déjà annulée")
        void throwsWhenAlreadyCancelled() {
            Invoice invoice = Invoice.create(issuer, client, dueDate, clock);
            invoice.cancel(clock);

            assertThatThrownBy(() -> invoice.cancel(laterClock))
                    .isInstanceOf(Invoice.IllegalTransitionException.class);
        }
    }
}
