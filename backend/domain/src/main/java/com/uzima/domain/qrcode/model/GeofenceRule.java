package com.uzima.domain.qrcode.model;

import java.util.Objects;

/**
 * Value Object : Règle de géofencing pour un QR Code contextuel.
 * Définit une zone géographique dans laquelle le QR Code est actif.
 * Le code n'est scannable que si l'utilisateur se trouve dans le rayon.
 * Invariants :
 * - latitude ∈ [-90, 90]
 * - longitude ∈ [-180, 180]
 * - radiusMeters > 0
 */
public record GeofenceRule(
        double latitude,
        double longitude,
        int    radiusMeters
) {

    public GeofenceRule {
        if (latitude < -90 || latitude > 90) {
            throw new InvalidGeofenceException("Latitude invalide : " + latitude + " (doit être entre -90 et 90)");
        }
        if (longitude < -180 || longitude > 180) {
            throw new InvalidGeofenceException("Longitude invalide : " + longitude + " (doit être entre -180 et 180)");
        }
        if (radiusMeters <= 0) {
            throw new InvalidGeofenceException("Le rayon doit être positif : " + radiusMeters);
        }
    }

    public static GeofenceRule of(double latitude, double longitude, int radiusMeters) {
        return new GeofenceRule(latitude, longitude, radiusMeters);
    }

    /**
     * Vérifie si une coordonnée se trouve dans la zone (approximation plane, suffisante pour ≤ 50 km).
     *
     * @param lat Latitude de la position à tester
     * @param lon Longitude de la position à tester
     */
    public boolean contains(double lat, double lon) {
        // Conversion degrés → mètres (approximation)
        double latDiff = Math.abs(latitude - lat) * 111_320;
        double lonDiff = Math.abs(longitude - lon) * 111_320 * Math.cos(Math.toRadians(latitude));
        double distance = Math.sqrt(latDiff * latDiff + lonDiff * lonDiff);
        return distance <= radiusMeters;
    }

    public static final class InvalidGeofenceException extends RuntimeException {
        public InvalidGeofenceException(String message) { super(message); }
    }
}
