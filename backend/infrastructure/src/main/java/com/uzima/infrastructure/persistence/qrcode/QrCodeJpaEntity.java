package com.uzima.infrastructure.persistence.qrcode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "qr_codes")
@Getter
@NoArgsConstructor
public class QrCodeJpaEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "max_scans")
    private Integer maxScans;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "scan_count", nullable = false)
    private int scanCount;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    // Géofencing (nullable)
    @Column(name = "geofence_latitude")
    private Double geofenceLatitude;

    @Column(name = "geofence_longitude")
    private Double geofenceLongitude;

    @Column(name = "geofence_radius_meters")
    private Integer geofenceRadiusMeters;

    // Personnalisation (nullable)
    @Column(name = "personalization_condition", length = 100)
    private String personalizationCondition;

    @Column(name = "personalization_target_profile", length = 100)
    private String personalizationTargetProfile;

    public static QrCodeJpaEntity of(
            UUID id, UUID ownerId, String type, Instant expiresAt, Integer maxScans,
            Instant createdAt, int scanCount, boolean revoked, Instant revokedAt,
            Double geofenceLatitude, Double geofenceLongitude, Integer geofenceRadiusMeters,
            String personalizationCondition, String personalizationTargetProfile
    ) {
        QrCodeJpaEntity e = new QrCodeJpaEntity();
        e.id                            = id;
        e.ownerId                       = ownerId;
        e.type                          = type;
        e.expiresAt                     = expiresAt;
        e.maxScans                      = maxScans;
        e.createdAt                     = createdAt;
        e.scanCount                     = scanCount;
        e.revoked                       = revoked;
        e.revokedAt                     = revokedAt;
        e.geofenceLatitude              = geofenceLatitude;
        e.geofenceLongitude             = geofenceLongitude;
        e.geofenceRadiusMeters          = geofenceRadiusMeters;
        e.personalizationCondition      = personalizationCondition;
        e.personalizationTargetProfile  = personalizationTargetProfile;
        return e;
    }
}
