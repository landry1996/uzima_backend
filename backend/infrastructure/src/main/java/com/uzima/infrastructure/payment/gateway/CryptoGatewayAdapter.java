package com.uzima.infrastructure.payment.gateway;

import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.port.PaymentGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Adaptateur production : Paiement en cryptomonnaie (stablecoins USDC/USDT).
 * <p>
 * Statut : Placeholder — intégration blockchain non encore implémentée.
 * <p>
 * Implémentation cible :
 * - USDC sur Polygon (frais réduits, transactions rapides)
 * - USDT sur Tron (large adoption en Afrique subsaharienne)
 * - SDK à utiliser : Web3j (Java) ou appel REST vers un nœud blockchain
 * <p>
 * Flux prévu :
 * 1. Création d'une adresse de dépôt temporaire pour la transaction
 * 2. Attente de confirmation on-chain (webhooks ou polling)
 * 3. Vérification du montant reçu (tolérance 1% pour les frais de gas)
 * 4. Confirmation ou annulation selon le résultat
 * <p>
 * Pour l'instant, toutes les transactions crypto sont refusées avec un message explicite.
 * Activer réellement lorsque l'intégration blockchain est disponible.
 */
public class CryptoGatewayAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(CryptoGatewayAdapter.class);

    @Override
    public GatewayResponse process(Transaction transaction) {
        String trackingId = "CRYPTO-PENDING-" + UUID.randomUUID();

        log.warn("[CRYPTO] Paiement crypto soumis mais non traité (intégration blockchain non disponible) "
                 + "→ txId={} amount={} trackingId={}",
                 transaction.id(), transaction.amount(), trackingId);

        // TODO : Intégrer Web3j ou un fournisseur de passerelle crypto
        // (Coinbase Commerce, BitPay, ou nœud Polygon/Tron direct)
        // Retourner GatewayResponse.success(txHash) une fois confirmé on-chain.
        return GatewayResponse.failure(
            "Paiement crypto temporairement indisponible. "
            + "L'intégration blockchain est en cours de déploiement. "
            + "Référence : " + trackingId
        );
    }
}
