package com.uzima.domain.qrcode.model;

/**
 * Type de QR Code contextuel (Innovation Brevetable #1 du cahier des charges).
 * Chaque type porte sa propre sémantique quant aux données exposées
 * et au comportement lors du scan.
 */
public enum QrCodeType {

    /** Réseau pro, CV, portfolio, prise de RDV */
    PROFESSIONAL("Professionnel"),

    /** Profil social, centres d'intérêt, ajout cercle amis */
    SOCIAL("Social"),

    /** Uniquement coordonnées paiement (données minimales, sécurité) */
    PAYMENT("Paiement"),

    /** Position GPS temporaire, durée limitée, auto-destruction */
    TEMPORARY_LOCATION("Localisation temporaire"),

    /** Badge événement, réseau privé temporaire, expire après l'événement */
    EVENT("Événement"),

    /** Groupe sanguin, allergies, contacts d'urgence - accessible offline */
    MEDICAL_EMERGENCY("Urgence médicale");

    private final String displayName;

    QrCodeType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * Règle métier : le QR code médical d'urgence doit pouvoir fonctionner
     * sans connexion internet (données encodées dans le QR lui-même).
     */
    public boolean requiresOfflineSupport() {
        return this == MEDICAL_EMERGENCY;
    }

    /**
     * Règle métier : certains types ont une expiration obligatoire.
     */
    public boolean expirationIsMandatory() {
        return this == TEMPORARY_LOCATION || this == EVENT;
    }
}
