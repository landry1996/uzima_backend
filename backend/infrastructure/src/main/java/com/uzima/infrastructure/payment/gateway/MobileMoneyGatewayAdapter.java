package com.uzima.infrastructure.payment.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.port.PaymentGatewayPort;
import com.uzima.infrastructure.shared.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.UUID;

/**
 * Adaptateur production : Paiement Mobile Money via MTN MoMo Open API.
 * <p>
 * Flux :
 * 1. Obtention d'un access token OAuth2 (Basic Auth sur subscription-key)
 * 2. POST /collection/v1_0/requesttopay avec X-Reference-Id généré
 * 3. La gateway répond 202 Accepted — le referenceId sert d'externalId
 * <p>
 * Note : MTN MoMo est asynchrone. Le résultat final (SUCCESS/FAILED) est
 * disponible via webhook ou polling sur GET /collection/v1_0/requesttopay/{referenceId}.
 * Ce polling doit être implémenté dans un job de réconciliation.
 * <p>
 * Configuration requise dans application-secret.yml :
 * <pre>
 *   uzima.payment.mobile-money.subscription-key: ...
 *   uzima.payment.mobile-money.api-user: ...
 *   uzima.payment.mobile-money.api-key: ...
 *   uzima.payment.mobile-money.base-url: https://proxy.momoapi.mtn.com
 *   uzima.payment.mobile-money.callback-url: https://api.uzima.app/webhooks/momo
 *   uzima.payment.mobile-money.environment: mtncameroon  # ou mtnghana, mtnivoire...
 * </pre>
 */
public class MobileMoneyGatewayAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(MobileMoneyGatewayAdapter.class);

    private final RestClient restClient;
    private final String subscriptionKey;
    private final String apiUser;
    private final String apiKey;
    private final String callbackUrl;
    private final String environment;

    public MobileMoneyGatewayAdapter(
            String baseUrl,
            String subscriptionKey,
            String apiUser,
            String apiKey,
            String callbackUrl,
            String environment
    ) {
        this.subscriptionKey = subscriptionKey;
        this.apiUser         = apiUser;
        this.apiKey          = apiKey;
        this.callbackUrl     = callbackUrl;
        this.environment     = environment;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Ocp-Apim-Subscription-Key", subscriptionKey)
                .build();
    }

    @Override
    public GatewayResponse process(Transaction transaction) {
        String referenceId = UUID.randomUUID().toString();

        try {
            String accessToken = obtainAccessToken();

            // Le payeur est identifié par son numéro de téléphone (MSISDN).
            // En production, résoudre le senderId en numéro via UserRepository.
            // Pour l'instant on utilise senderId comme identifiant externe.
            String payerMsisdn = transaction.senderId().toString();

            RequestToPayBody body = new RequestToPayBody(
                transaction.amount().amount().toPlainString(),
                transaction.amount().currency().name(),
                transaction.id().value().toString(),
                new MoMoParty("MSISDN", payerMsisdn),
                "Paiement Uzima",
                "Transfert via Uzima"
            );

            restClient.post()
                    .uri("/collection/v1_0/requesttopay")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Reference-Id", referenceId)
                    .header("X-Target-Environment", environment)
                    .header("X-Callback-Url", callbackUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity(); // 202 Accepted

            log.info("[MOBILE_MONEY] RequestToPay soumis → referenceId={} txId={}",
                     referenceId, transaction.id());
            return GatewayResponse.success(referenceId);

        } catch (RestClientException ex) {
            log.error("[MOBILE_MONEY] Échec de la soumission → txId={}", transaction.id(), ex);
            throw new ExternalServiceException("MTN MoMo", "Échec du paiement Mobile Money", ex);
        }
    }

    private String obtainAccessToken() {
        String credentials = Base64.getEncoder()
                .encodeToString((apiUser + ":" + apiKey).getBytes());

        TokenResponse response = restClient.post()
                .uri("/collection/token/")
                .header("Authorization", "Basic " + credentials)
                .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new ExternalServiceException("MTN MoMo",
                "Impossible d'obtenir un access token OAuth2", null);
        }
        return response.accessToken();
    }

    // -------------------------------------------------------------------------
    // DTOs internes (MTN MoMo API)
    // -------------------------------------------------------------------------

    record RequestToPayBody(
            String amount,
            String currency,
            String externalId,
            MoMoParty payer,
            String payerMessage,
            String payeeNote
    ) {}

    record MoMoParty(String partyIdType, String partyId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type")   String tokenType
    ) {}
}
