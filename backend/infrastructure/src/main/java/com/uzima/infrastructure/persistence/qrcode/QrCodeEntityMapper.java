package com.uzima.infrastructure.persistence.qrcode;

import com.uzima.domain.qrcode.model.*;
import com.uzima.domain.user.model.UserId;

class QrCodeEntityMapper {

    QrCode toDomain(QrCodeJpaEntity e) {
        ExpirationPolicy expiry = e.getExpiresAt() != null
            ? ExpirationPolicy.reconstitute(e.getExpiresAt())
            : ExpirationPolicy.permanent();

        ScanLimit scanLimit = e.getMaxScans() != null
            ? ScanLimit.of(e.getMaxScans())
            : ScanLimit.unlimited();

        GeofenceRule geofence = null;
        if (e.getGeofenceLatitude() != null && e.getGeofenceLongitude() != null
                && e.getGeofenceRadiusMeters() != null) {
            geofence = GeofenceRule.of(
                e.getGeofenceLatitude(), e.getGeofenceLongitude(), e.getGeofenceRadiusMeters()
            );
        }

        PersonalizationRule personalization = null;
        if (e.getPersonalizationCondition() != null && e.getPersonalizationTargetProfile() != null) {
            personalization = PersonalizationRule.of(
                e.getPersonalizationCondition(), e.getPersonalizationTargetProfile()
            );
        }

        return QrCode.reconstitute(
            QrCodeId.of(e.getId()),
            UserId.of(e.getOwnerId()),
            QrCodeType.valueOf(e.getType()),
            expiry,
            scanLimit,
            e.getCreatedAt(),
            e.getScanCount(),
            e.isRevoked(),
            e.getRevokedAt(),
            geofence,
            personalization
        );
    }

    QrCodeJpaEntity toJpaEntity(QrCode q) {
        return QrCodeJpaEntity.of(
            q.id().value(),
            q.ownerId().value(),
            q.type().name(),
            q.expirationPolicy().expiresAt().orElse(null),
            q.scanLimit().maxScans().isPresent() ? q.scanLimit().maxScans().getAsInt() : null,
            q.createdAt(),
            q.scanCount(),
            q.isRevoked(),
            q.revokedAt().orElse(null),
            q.geofenceRule().map(GeofenceRule::latitude).orElse(null),
            q.geofenceRule().map(GeofenceRule::longitude).orElse(null),
            q.geofenceRule().map(GeofenceRule::radiusMeters).orElse(null),
            q.personalizationRule().map(PersonalizationRule::condition).orElse(null),
            q.personalizationRule().map(PersonalizationRule::targetProfile).orElse(null)
        );
    }
}
