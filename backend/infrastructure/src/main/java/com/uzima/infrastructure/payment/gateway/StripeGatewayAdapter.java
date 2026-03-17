package com.uzima.infrastructure.payment.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.port.PaymentGatewayPort;
import com.uzima.infrastructure.shared.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptateur production : Paiement par carte bancaire via Stripe API.
 * <p>
 * Flux :
 * 1. POST /v1/payment_intents avec amount, currency, payment_method
 * 2. Stripe répond synchroniquement avec un statut (succeeded / requires_action)
 * 3. Retourne GatewayResponse.success(paymentIntentId) si status = succeeded
 * <p>
 * Note : Les paiements nécessitant une authentification 3D Secure (requires_action)
 * doivent être traités côté frontend via Stripe.js. Ce cas doit être géré dans
 * un flux de confirmation côté client (redirect vers payment_intent.next_action.url).
 * <p>
 * Configuration requise dans application-secret.yml :
 * <pre>
 *   uzima.payment.stripe.secret-key: sk_live_...
 *   uzima.payment.stripe.payment-method-id: pm_card_...  # optionnel en prod
 * </pre>
 */
public class StripeGatewayAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(StripeGatewayAdapter.class);
    private static final String STRIPE_BASE_URL = "https://api.stripe.com";

    private final RestClient restClient;

    public StripeGatewayAdapter(String secretKey) {
        this.restClient = RestClient.builder()
                .baseUrl(STRIPE_BASE_URL)
                .defaultHeader("Authorization", "Bearer " + secretKey)
                .build();
    }

    @Override
    public GatewayResponse process(Transaction transaction) {
        try {
            // Stripe attend le montant en centimes (entier)
            long amountInCents = transaction.amount().amount()
                    .movePointRight(2)
                    .longValue();

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("amount", String.valueOf(amountInCents));
            body.add("currency", transaction.amount().currency().name().toLowerCase());
            body.add("confirm", "true");
            body.add("payment_method_types[]", "card");
            body.add("metadata[uzima_tx_id]", transaction.id().value().toString());

            transaction.description().ifPresent(desc ->
                body.add("description", desc)
            );

            PaymentIntentResponse response = restClient.post()
                    .uri("/v1/payment_intents")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(PaymentIntentResponse.class);

            if (response == null || response.id() == null) {
                throw new ExternalServiceException("Stripe",
                    "Réponse Stripe vide pour txId=" + transaction.id(), null);
            }

            if ("succeeded".equals(response.status())) {
                log.info("[STRIPE] Paiement réussi → paymentIntentId={} txId={}",
                         response.id(), transaction.id());
                return GatewayResponse.success(response.id());
            }

            // requires_action, requires_payment_method, etc.
            log.warn("[STRIPE] Paiement en attente de confirmation → status={} txId={}",
                     response.status(), transaction.id());
            return GatewayResponse.failure("Paiement Stripe en attente : " + response.status());

        } catch (RestClientException ex) {
            log.error("[STRIPE] Échec de la soumission → txId={}", transaction.id(), ex);
            throw new ExternalServiceException("Stripe", "Échec du paiement par carte", ex);
        }
    }

    // -------------------------------------------------------------------------
    // DTOs internes (Stripe API)
    // -------------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PaymentIntentResponse(
            String id,
            String status,
            @JsonProperty("client_secret") String clientSecret
    ) {}
}
