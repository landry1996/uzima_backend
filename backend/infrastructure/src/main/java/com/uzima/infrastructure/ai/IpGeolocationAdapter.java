package com.uzima.infrastructure.ai;

import com.uzima.application.qrcode.port.out.GeolocationPort;
import com.uzima.domain.user.model.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Adaptateur production : Géolocalisation.
 * <p>
 * La géolocalisation par IP/GPS nécessite soit :
 * <ul>
 *   <li>L'adresse IP de la requête HTTP (disponible dans le filtre, pas dans le domaine)</li>
 *   <li>Des coordonnées GPS envoyées par le client mobile</li>
 * </ul>
 * Le port {@link GeolocationPort} reçoit uniquement un {@code UserId}, ce qui ne suffit pas
 * pour résoudre une localisation sans stocker la dernière position connue.
 * <p>
 * Cette implémentation retourne {@link Optional#empty()} jusqu'à ce qu'un mécanisme
 * de remontée de position soit implémenté (ex. endpoint POST /api/users/location).
 */
public class IpGeolocationAdapter implements GeolocationPort {

    private static final Logger log = LoggerFactory.getLogger(IpGeolocationAdapter.class);

    @Override
    public Optional<UserLocation> getCurrentLocation(UserId userId) {
        log.debug("[GEOLOCATION] Localisation non disponible pour l'utilisateur {} — "
                + "le client n'a pas encore remonté sa position", userId);
        return Optional.empty();
    }
}
