package com.uzima.infrastructure.payment.gateway;

import com.uzima.domain.payment.model.Money;
import com.uzima.domain.payment.model.Transaction;
import com.uzima.domain.payment.model.Wallet;
import com.uzima.domain.payment.port.PaymentGatewayPort;
import com.uzima.domain.payment.port.WalletRepositoryPort;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.infrastructure.shared.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Adaptateur production : Paiement via le portefeuille interne Uzima.
 * <p>
 * Flux :
 * 1. Chargement du portefeuille de l'expéditeur — lève une erreur si absent
 * 2. Débit du solde expéditeur via {@link Wallet#debit(Money, TimeProvider)}
 *    (lève {@link Money.InsufficientFundsException} si solde insuffisant)
 * 3. Chargement ou création du portefeuille du destinataire
 * 4. Crédit du solde destinataire via {@link Wallet#credit(Money, TimeProvider)}
 * 5. Persistance des deux portefeuilles
 * 6. Retourne un {@code WALLET-{UUID}} comme identifiant de référence interne
 */
public class WalletGatewayAdapter implements PaymentGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(WalletGatewayAdapter.class);

    private final WalletRepositoryPort walletRepository;
    private final TimeProvider clock;

    public WalletGatewayAdapter(WalletRepositoryPort walletRepository, TimeProvider clock) {
        this.walletRepository = walletRepository;
        this.clock            = clock;
    }

    @Override
    public GatewayResponse process(Transaction transaction) {
        String internalReferenceId = "WALLET-" + UUID.randomUUID();

        log.info("[WALLET] Traitement du paiement interne → txId={} sender={} recipient={} amount={}",
                 transaction.id(),
                 transaction.senderId(),
                 transaction.recipientId(),
                 transaction.amount());

        try {
            // 1. Charger le portefeuille de l'expéditeur
            Wallet senderWallet = walletRepository.findByOwnerId(transaction.senderId())
                    .orElseThrow(() -> new ExternalServiceException(
                        "Wallet",
                        "Portefeuille introuvable pour l'expéditeur : " + transaction.senderId(),
                        null
                    ));

            // 2. Débiter l'expéditeur (lève InsufficientFundsException si solde insuffisant)
            senderWallet.debit(transaction.amount(), clock);

            // 3. Charger ou créer le portefeuille du destinataire
            Wallet recipientWallet = walletRepository.findByOwnerId(transaction.recipientId())
                    .orElseGet(() -> {
                        log.info("[WALLET] Création d'un portefeuille pour le destinataire {}",
                                 transaction.recipientId());
                        return Wallet.create(transaction.recipientId(),
                                             transaction.amount().currency(), clock);
                    });

            // 4. Créditer le destinataire
            recipientWallet.credit(transaction.amount(), clock);

            // 5. Persister les deux portefeuilles
            walletRepository.save(senderWallet);
            walletRepository.save(recipientWallet);

            log.info("[WALLET] Paiement interne effectué → referenceId={} senderBalance={} recipientBalance={}",
                     internalReferenceId, senderWallet.balance(), recipientWallet.balance());
            return GatewayResponse.success(internalReferenceId);

        } catch (Money.InsufficientFundsException ex) {
            log.warn("[WALLET] Solde insuffisant → txId={} sender={} amount={}",
                     transaction.id(), transaction.senderId(), transaction.amount());
            return GatewayResponse.failure("Solde insuffisant : " + ex.getMessage());
        }
    }
}
