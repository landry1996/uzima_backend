package com.uzima.infrastructure.payment.gateway;

import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.port.PaymentGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Adaptateur de paiement DÉSACTIVÉ — profil local/test ({@code !prod}).
 * <p>
 * Aucune simulation métier. Aucun succès ou échec artificiels.
 * Lève une exception explicite pour signaler que la gateway n'est pas configurée.
 * <p>
 * Objectif : rendre visible immédiatement en développement qu'aucune gateway
 * réelle n'est connectée, sans polluer les tests avec du comportement fictif.
 * <p>
 * En production, remplacer par l'un des adapters suivants selon la méthode :
 * <ul>
 *   <li>{@code MobileMoneyGatewayAdapter} — Orange Money, MTN MoMo, Wave</li>
 *   <li>{@code StripeGatewayAdapter}      — Carte bancaire (Stripe API)</li>
 *   <li>{@code WalletGatewayAdapter}      — Portefeuille interne Uzima</li>
 *   <li>{@code CryptoGatewayAdapter}      — Stablecoins USDC/USDT</li>
 * </ul>
 * Ces adapters sont à créer lorsque les clés API correspondantes sont disponibles.
 */
public final class DisabledPaymentGatewayAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(DisabledPaymentGatewayAdapter.class);

    @Override
    public GatewayResponse process(Transaction transaction) {
        Objects.requireNonNull(transaction);
        log.error("[PAYMENT] Gateway non configurée — transaction refusée → id={} method={} amount={}",
                transaction.id(), transaction.method().displayName(), transaction.amount());
        throw new PaymentGatewayNotConfiguredException(
                "Aucune gateway de paiement n'est configurée pour l'environnement courant. "
                + "Méthode demandée : " + transaction.method().displayName() + ". "
                + "Activez le profil 'prod' et configurez les clés API dans application-secret.yml."
        );
    }

    /**
     * Exception levée lorsqu'aucune gateway réelle n'est connectée.
     * Distincte d'un échec de paiement normal — c'est une erreur de configuration.
     */
    public static final class PaymentGatewayNotConfiguredException extends RuntimeException {
        public PaymentGatewayNotConfiguredException(String message) {
            super(message);
        }
    }
}
