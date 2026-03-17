package com.uzima.infrastructure.ai;

import com.uzima.application.qrcode.port.out.GeolocationPort;
import com.uzima.domain.user.model.UserId;

import java.util.Optional;

/**
 * Adaptateur stub : Géolocalisation no-op (pour dev/test).
 * <p>
 * Retourne toujours empty — pas de restriction géographique en dev.
 * Remplacer par un vrai adaptateur GPS/IP en production.
 */
public class StubGeolocationAdapter implements GeolocationPort {

    @Override
    public Optional<UserLocation> getCurrentLocation(UserId userId) {
        // Dev : pas de géolocalisation disponible
        return Optional.empty();
    }
}
