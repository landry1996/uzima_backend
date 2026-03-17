package com.uzima.application.qrcode.port.out;

import com.uzima.domain.user.model.UserId;

/** Port OUT : Service de géolocalisation. */
public interface GeolocationPort {

    /**
     * Retourne la position actuelle de l'utilisateur.
     *
     * @param userId Identifiant de l'utilisateur
     * @return Position actuelle ou absent si non disponible
     */
    java.util.Optional<UserLocation> getCurrentLocation(UserId userId);

    record UserLocation(double latitude, double longitude) {
        public static UserLocation of(double latitude, double longitude) {
            return new UserLocation(latitude, longitude);
        }
    }
}
