package com.uzima.domain.payment.model;

/**
 * Enum : Méthodes de paiement supportées.
 * MOBILE_MONEY : Orange Money, MTN MoMo, Wave, etc.
 * CARD         : Carte bancaire (Visa, Mastercard)
 * CRYPTO       : Stablecoins USDC/USDT sur blockchain légère
 * WALLET       : Portefeuille interne Uzima (solde applicatif)
 */
public enum PaymentMethod {

    MOBILE_MONEY("Mobile Money"),
    CARD("Carte bancaire"),
    CRYPTO("Crypto-monnaie"),
    WALLET("Portefeuille Uzima");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
