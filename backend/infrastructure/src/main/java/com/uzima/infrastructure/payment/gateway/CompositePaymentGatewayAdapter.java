package com.uzima.infrastructure.payment.gateway;

import com.uzima.domain.payment.model.PaymentMethod;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.port.PaymentGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * Adaptateur composite : Routage vers la bonne gateway selon la méthode de paiement.
 * <p>
 * Pattern Composite + Strategy :
 * - Ce bean est l'unique {@link PaymentGatewayPort} exposé au domaine applicatif.
 * - Il délègue à la gateway spécialisée via une {@code Map<PaymentMethod, PaymentGatewayPort>}.
 * <p>
 * Routage :
 * <ul>
 *   <li>MOBILE_MONEY → {@link MobileMoneyGatewayAdapter} (MTN MoMo Open API)</li>
 *   <li>CARD         → {@link StripeGatewayAdapter} (Stripe Payment Intents API)</li>
 *   <li>WALLET       → {@link WalletGatewayAdapter} (portefeuille interne Uzima)</li>
 *   <li>CRYPTO       → {@link CryptoGatewayAdapter} (stablecoins, placeholder)</li>
 * </ul>
 * <p>
 * Câblé dans {@code InfrastructureConfiguration} avec {@code @Profile("prod")}.
 */
public class CompositePaymentGatewayAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(CompositePaymentGatewayAdapter.class);

    private final Map<PaymentMethod, PaymentGatewayPort> gateways;

    public CompositePaymentGatewayAdapter(Map<PaymentMethod, PaymentGatewayPort> gateways) {
        this.gateways = Objects.requireNonNull(gateways, "La map des gateways est obligatoire");
    }

    @Override
    public GatewayResponse process(Transaction transaction) {
        Objects.requireNonNull(transaction, "La transaction est obligatoire");

        PaymentMethod method = transaction.method();
        PaymentGatewayPort gateway = gateways.get(method);

        if (gateway == null) {
            log.error("[COMPOSITE_GATEWAY] Aucune gateway configurée pour la méthode {} → txId={}",
                      method.displayName(), transaction.id());
            return GatewayResponse.failure(
                "Méthode de paiement non supportée : " + method.displayName()
            );
        }

        log.debug("[COMPOSITE_GATEWAY] Routage → {} pour txId={}",
                  gateway.getClass().getSimpleName(), transaction.id());
        return gateway.process(transaction);
    }
}
