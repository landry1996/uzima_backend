package com.uzima.domain.qrcode;

import com.uzima.domain.qrcode.model.*;
import com.uzima.domain.qrcode.specification.*;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires du domaine QrCode.
 * Aucun Spring, aucune DB, aucun framework.
 */
class QrCodeTest {

    private static final Instant NOW = Instant.parse("2026-03-12T10:00:00Z");
    private static final Instant PAST = NOW.minus(Duration.ofHours(1));
    private static final Instant FUTURE = NOW.plus(Duration.ofHours(24));

    private final TimeProvider clock = () -> NOW;
    private UserId owner;

    @BeforeEach
    void setUp() {
        owner = UserId.generate();
    }

    @Nested
    @DisplayName("QrCode.reconstitute() et createdAt()")
    class ReconstitueTest {

        @Test
        @DisplayName("reconstitue un QR Code depuis la persistance avec les bonnes valeurs")
        void reconstituePreservesAllFields() {
            QrCodeId id = QrCodeId.generate();
            ExpirationPolicy policy = ExpirationPolicy.permanent();
            ScanLimit limit = ScanLimit.of(10);

            QrCode qrCode = QrCode.reconstitute(
                    id, owner, QrCodeType.PROFESSIONAL,
                    policy, limit,
                    NOW,       // createdAt
                    5,         // scanCount
                    false,     // revoked
                    null       // revokedAt
            );

            assertThat(qrCode.id()).isEqualTo(id);
            assertThat(qrCode.ownerId()).isEqualTo(owner);
            assertThat(qrCode.type()).isEqualTo(QrCodeType.PROFESSIONAL);
            assertThat(qrCode.scanCount()).isEqualTo(5);
            assertThat(qrCode.isRevoked()).isFalse();
            // createdAt() préserve l'instant original de persistance
            assertThat(qrCode.createdAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("reconstitue un QR Code révoqué avec sa date de révocation")
        void reconstitueRevokedQrCode() {
            QrCode qrCode = QrCode.reconstitute(
                    QrCodeId.generate(), owner, QrCodeType.SOCIAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(),
                    NOW, 0, true, NOW
            );

            assertThat(qrCode.isRevoked()).isTrue();
            assertThat(qrCode.revokedAt()).hasValue(NOW);
            assertThat(qrCode.isActiveAt(NOW)).isFalse();
        }
    }

    @Nested
    @DisplayName("ExpirationPolicy.expiresAt()")
    class ExpiresAtTest {

        @Test
        @DisplayName("expiresAt() retourne Optional.empty() pour une politique permanente")
        void permanentPolicyHasNoExpiresAt() {
            var policy = ExpirationPolicy.permanent();
            assertThat(policy.expiresAt()).isEmpty();
        }

        @Test
        @DisplayName("expiresAt() retourne l'instant d'expiration pour une politique temporelle")
        void timedPolicyExposesExpiresAt() {
            var policy = ExpirationPolicy.expiresAt(FUTURE, NOW);
            assertThat(policy.expiresAt()).hasValue(FUTURE);
        }

        @Test
        @DisplayName("expiresAt() retourne la date correcte après expiresAfter()")
        void expiresAfterExposesCorrectInstant() {
            Duration duration = Duration.ofHours(24);
            var policy = ExpirationPolicy.expiresAfter(duration, NOW);
            assertThat(policy.expiresAt()).hasValue(NOW.plus(duration));
        }
    }

    @Nested
    @DisplayName("Specification atomiques — not() et or()")
    class SpecificationCompositionTest {

        @Test
        @DisplayName("QrCodeRevokedSpecification.not() : actif sur QR non révoqué")
        void notRevokedSpecificationOnActiveQr() {
            var qr = QrCode.create(owner, QrCodeType.PROFESSIONAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);

            var notRevoked = new QrCodeRevokedSpecification().not();

            assertThat(notRevoked.isSatisfiedBy(qr)).isTrue();
        }

        @Test
        @DisplayName("QrCodeRevokedSpecification.not() : échoue sur QR révoqué")
        void notRevokedSpecificationOnRevokedQr() {
            var qr = QrCode.create(owner, QrCodeType.PROFESSIONAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);
            qr.revoke(clock);

            var notRevoked = new QrCodeRevokedSpecification().not();

            assertThat(notRevoked.isSatisfiedBy(qr)).isFalse();
        }

        @Test
        @DisplayName("QrCodeExpiredSpecification : détecte un QR expiré")
        void expiredSpecificationDetectsExpiredQr() {
            var policy = ExpirationPolicy.expiresAt(FUTURE, NOW);
            var qr = QrCode.create(owner, QrCodeType.PAYMENT, policy, ScanLimit.unlimited(), clock);

            assertThat(new QrCodeExpiredSpecification(FUTURE).isSatisfiedBy(qr)).isTrue();
            assertThat(new QrCodeExpiredSpecification(NOW).isSatisfiedBy(qr)).isFalse();
        }

        @Test
        @DisplayName("QrCodeScanLimitReachedSpecification : détecte la limite atteinte")
        void scanLimitReachedSpecification() {
            var qr = QrCode.create(owner, QrCodeType.PAYMENT,
                    ExpirationPolicy.permanent(), ScanLimit.of(2), clock);
            qr.recordScan(NOW);
            qr.recordScan(NOW);

            assertThat(new QrCodeScanLimitReachedSpecification().isSatisfiedBy(qr)).isTrue();
        }

        @Test
        @DisplayName("QrCodeIsActiveSpecification : composition not().and() valide un QR actif")
        void isActiveSpecificationValidatesActiveQr() {
            var qr = QrCode.create(owner, QrCodeType.SOCIAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);

            var spec = new QrCodeIsActiveSpecification(NOW);

            assertThat(spec.isSatisfiedBy(qr)).isTrue();
        }

        @Test
        @DisplayName("QrCodeIsActiveSpecification : composition not().and() rejette un QR révoqué")
        void isActiveSpecificationRejectsRevokedQr() {
            var qr = QrCode.create(owner, QrCodeType.SOCIAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);
            qr.revoke(clock);

            assertThat(new QrCodeIsActiveSpecification(NOW).isSatisfiedBy(qr)).isFalse();
            assertThat(new QrCodeIsActiveSpecification(NOW).failureReason(qr))
                    .isEqualTo("QR_CODE_REVOKED");
        }

        @Test
        @DisplayName("QrCodeInvalidSpecification : composition or() détecte tout QR invalide")
        void invalidSpecificationViaOrComposition() {
            // QR révoqué
            var revoked = QrCode.create(owner, QrCodeType.SOCIAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);
            revoked.revoke(clock);

            // QR expiré
            var expired = QrCode.create(owner, QrCodeType.PAYMENT,
                    ExpirationPolicy.expiresAt(FUTURE, NOW), ScanLimit.unlimited(), clock);

            // QR actif
            var active = QrCode.create(owner, QrCodeType.PROFESSIONAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);

            var invalidSpec = new QrCodeInvalidSpecification(FUTURE);

            assertThat(invalidSpec.isSatisfiedBy(revoked)).isTrue();  // révoqué → invalide
            assertThat(invalidSpec.isSatisfiedBy(expired)).isTrue();  // expiré → invalide
            assertThat(invalidSpec.isSatisfiedBy(active)).isFalse();  // actif → valide
        }
    }

    @Nested
    @DisplayName("ExpirationPolicy Value Object")
    class ExpirationPolicyTest {

        @Test
        @DisplayName("une politique permanente n'expire jamais")
        void permanentNeverExpires() {
            var policy = ExpirationPolicy.permanent();
            assertThat(policy.isPermanent()).isTrue();
            assertThat(policy.isExpiredAt(Instant.MAX)).isFalse();
        }

        @Test
        @DisplayName("une politique avec date expire correctement")
        void timedPolicyExpires() {
            var policy = ExpirationPolicy.expiresAt(FUTURE, NOW);
            assertThat(policy.isExpiredAt(NOW)).isFalse();
            assertThat(policy.isExpiredAt(FUTURE)).isTrue();
            assertThat(policy.isExpiredAt(FUTURE.plusSeconds(1))).isTrue();
        }

        @Test
        @DisplayName("rejette une date d'expiration dans le passé")
        void rejectsPastExpiration() {
            assertThatThrownBy(() -> ExpirationPolicy.expiresAt(PAST, NOW))
                .isInstanceOf(ExpirationPolicy.ExpirationInThePastException.class);
        }

        @Test
        @DisplayName("expiresAfter crée une politique relative à maintenant")
        void expiresAfterDuration() {
            var policy = ExpirationPolicy.expiresAfter(Duration.ofHours(24), NOW);
            assertThat(policy.isExpiredAt(NOW.plus(Duration.ofHours(23)))).isFalse();
            assertThat(policy.isExpiredAt(NOW.plus(Duration.ofHours(25)))).isTrue();
        }
    }

    @Nested
    @DisplayName("ScanLimit Value Object")
    class ScanLimitTest {

        @Test
        @DisplayName("limite illimitée ne bloque jamais")
        void unlimitedNeverBlocks() {
            var limit = ScanLimit.unlimited();
            assertThat(limit.isReachedBy(Integer.MAX_VALUE)).isFalse();
        }

        @Test
        @DisplayName("limite fixe bloque quand atteinte")
        void fixedLimitBlocks() {
            var limit = ScanLimit.of(3);
            assertThat(limit.isReachedBy(2)).isFalse();
            assertThat(limit.isReachedBy(3)).isTrue();
            assertThat(limit.isReachedBy(4)).isTrue();
        }

        @Test
        @DisplayName("rejette une limite inférieure à 1")
        void rejectsZeroLimit() {
            assertThatThrownBy(() -> ScanLimit.of(0))
                .isInstanceOf(ScanLimit.InvalidScanLimitException.class);
        }
    }

    @Nested
    @DisplayName("QrCode.create()")
    class QrCodeCreateTest {

        @Test
        @DisplayName("crée un QR Code professionnel permanent")
        void createsProfessionalPermanentQrCode() {
            var qr = QrCode.create(owner, QrCodeType.PROFESSIONAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);

            assertThat(qr.id()).isNotNull();
            assertThat(qr.ownerId()).isEqualTo(owner);
            assertThat(qr.type()).isEqualTo(QrCodeType.PROFESSIONAL);
            assertThat(qr.isRevoked()).isFalse();
            assertThat(qr.scanCount()).isEqualTo(0);
            assertThat(qr.isActiveAt(NOW)).isTrue();
        }

        @Test
        @DisplayName("TEMPORARY_LOCATION exige une date d'expiration")
        void temporaryLocationRequiresExpiration() {
            assertThatThrownBy(() -> QrCode.create(
                    owner,
                    QrCodeType.TEMPORARY_LOCATION,
                    ExpirationPolicy.permanent(), // interdit pour ce type
                    ScanLimit.unlimited(),
                    clock
            )).isInstanceOf(QrCode.ExpirationRequiredForTypeException.class);
        }

        @Test
        @DisplayName("EVENT exige une date d'expiration")
        void eventRequiresExpiration() {
            assertThatThrownBy(() -> QrCode.create(
                    owner,
                    QrCodeType.EVENT,
                    ExpirationPolicy.permanent(),
                    ScanLimit.unlimited(),
                    clock
            )).isInstanceOf(QrCode.ExpirationRequiredForTypeException.class);
        }

        @Test
        @DisplayName("crée un QR Code TEMPORARY_LOCATION avec expiration valide")
        void createsTemporaryLocationWithExpiration() {
            var policy = ExpirationPolicy.expiresAfter(Duration.ofHours(2), NOW);
            var qr = QrCode.create(owner, QrCodeType.TEMPORARY_LOCATION, policy, ScanLimit.of(1), clock);
            assertThat(qr.isActiveAt(NOW)).isTrue();
        }
    }

    @Nested
    @DisplayName("QrCode.recordScan()")
    class RecordScanTest {

        @Test
        @DisplayName("enregistre un scan correctement")
        void recordsScan() {
            var qr = QrCode.create(owner, QrCodeType.SOCIAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);
            qr.recordScan(NOW);
            assertThat(qr.scanCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("rejette un scan sur un QR révoqué")
        void rejectsScanOnRevokedQr() {
            var qr = QrCode.create(owner, QrCodeType.SOCIAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);
            qr.revoke(clock);

            assertThatThrownBy(() -> qr.recordScan(NOW))
                .isInstanceOf(QrCode.QrCodeRevokedException.class);
        }

        @Test
        @DisplayName("rejette un scan sur un QR expiré")
        void rejectsScanOnExpiredQr() {
            var policy = ExpirationPolicy.expiresAfter(Duration.ofHours(1), NOW);
            var qr = QrCode.create(owner, QrCodeType.PAYMENT, policy, ScanLimit.unlimited(), clock);

            Instant afterExpiry = NOW.plus(Duration.ofHours(2));
            assertThatThrownBy(() -> qr.recordScan(afterExpiry))
                .isInstanceOf(QrCode.QrCodeExpiredException.class);
        }

        @Test
        @DisplayName("rejette un scan quand la limite est atteinte")
        void rejectsScanWhenLimitReached() {
            var qr = QrCode.create(owner, QrCodeType.PAYMENT,
                    ExpirationPolicy.permanent(), ScanLimit.of(2), clock);
            qr.recordScan(NOW);
            qr.recordScan(NOW);

            assertThatThrownBy(() -> qr.recordScan(NOW))
                .isInstanceOf(QrCode.ScanLimitReachedException.class);
        }
    }

    @Nested
    @DisplayName("QrCode.revoke()")
    class RevokeTest {

        @Test
        @DisplayName("révoque le QR Code")
        void revokesQrCode() {
            var qr = QrCode.create(owner, QrCodeType.PROFESSIONAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);
            qr.revoke(clock);

            assertThat(qr.isRevoked()).isTrue();
            assertThat(qr.revokedAt()).hasValue(NOW);
            assertThat(qr.isActiveAt(NOW)).isFalse();
        }

        @Test
        @DisplayName("la révocation est idempotente")
        void revocationIsIdempotent() {
            var qr = QrCode.create(owner, QrCodeType.PROFESSIONAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);
            assertThatNoException().isThrownBy(() -> {
                qr.revoke(clock);
                qr.revoke(clock);
            });
        }
    }

    // =========================================================================
    // GeofenceRule
    // =========================================================================

    @Nested
    @DisplayName("GeofenceRule — validation et contains()")
    class GeofenceRuleTest {

        @Test
        @DisplayName("crée une règle valide avec les bons champs")
        void createsValidRule() {
            var rule = GeofenceRule.of(48.8566, 2.3522, 500);
            assertThat(rule.latitude()).isEqualTo(48.8566);
            assertThat(rule.longitude()).isEqualTo(2.3522);
            assertThat(rule.radiusMeters()).isEqualTo(500);
        }

        @Test
        @DisplayName("rejette une latitude hors [-90, 90]")
        void rejectsInvalidLatitude() {
            assertThatThrownBy(() -> GeofenceRule.of(91.0, 0.0, 100))
                .isInstanceOf(GeofenceRule.InvalidGeofenceException.class);
            assertThatThrownBy(() -> GeofenceRule.of(-91.0, 0.0, 100))
                .isInstanceOf(GeofenceRule.InvalidGeofenceException.class);
        }

        @Test
        @DisplayName("rejette une longitude hors [-180, 180]")
        void rejectsInvalidLongitude() {
            assertThatThrownBy(() -> GeofenceRule.of(0.0, 181.0, 100))
                .isInstanceOf(GeofenceRule.InvalidGeofenceException.class);
            assertThatThrownBy(() -> GeofenceRule.of(0.0, -181.0, 100))
                .isInstanceOf(GeofenceRule.InvalidGeofenceException.class);
        }

        @Test
        @DisplayName("rejette un rayon nul ou négatif")
        void rejectsNonPositiveRadius() {
            assertThatThrownBy(() -> GeofenceRule.of(0.0, 0.0, 0))
                .isInstanceOf(GeofenceRule.InvalidGeofenceException.class);
            assertThatThrownBy(() -> GeofenceRule.of(0.0, 0.0, -1))
                .isInstanceOf(GeofenceRule.InvalidGeofenceException.class);
        }

        @Test
        @DisplayName("contains() retourne true pour un point dans la zone")
        void containsPointInsideZone() {
            // Paris — rayon 1000 m
            var rule = GeofenceRule.of(48.8566, 2.3522, 1000);
            // Point à ~200 m
            assertThat(rule.contains(48.8576, 2.3522)).isTrue();
        }

        @Test
        @DisplayName("contains() retourne false pour un point hors de la zone")
        void rejectsPointOutsideZone() {
            // Paris — rayon 100 m
            var rule = GeofenceRule.of(48.8566, 2.3522, 100);
            // Lyon (~400 km)
            assertThat(rule.contains(45.7640, 4.8357)).isFalse();
        }

        @Test
        @DisplayName("contains() retourne true pour le centre exact")
        void containsCenter() {
            var rule = GeofenceRule.of(5.3600, -4.0083, 50);
            assertThat(rule.contains(5.3600, -4.0083)).isTrue();
        }
    }

    // =========================================================================
    // PersonalizationRule
    // =========================================================================

    @Nested
    @DisplayName("PersonalizationRule — validation et factory")
    class PersonalizationRuleTest {

        @Test
        @DisplayName("crée une règle valide")
        void createsValidRule() {
            var rule = PersonalizationRule.of("WORK_HOURS", "COLLEAGUE");
            assertThat(rule.condition()).isEqualTo("WORK_HOURS");
            assertThat(rule.targetProfile()).isEqualTo("COLLEAGUE");
        }

        @Test
        @DisplayName("alwaysForAnyone() retourne ALWAYS / ANYONE")
        void alwaysForAnyoneFactory() {
            var rule = PersonalizationRule.alwaysForAnyone();
            assertThat(rule.condition()).isEqualTo("ALWAYS");
            assertThat(rule.targetProfile()).isEqualTo("ANYONE");
        }

        @Test
        @DisplayName("rejette une condition null")
        void rejectsNullCondition() {
            assertThatThrownBy(() -> PersonalizationRule.of(null, "ANYONE"))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejette un targetProfile null")
        void rejectsNullTargetProfile() {
            assertThatThrownBy(() -> PersonalizationRule.of("ALWAYS", null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejette une condition vide ou blank")
        void rejectsBlankCondition() {
            assertThatThrownBy(() -> PersonalizationRule.of("  ", "ANYONE"))
                .isInstanceOf(PersonalizationRule.InvalidPersonalizationRuleException.class);
        }

        @Test
        @DisplayName("rejette un targetProfile vide ou blank")
        void rejectsBlankTargetProfile() {
            assertThatThrownBy(() -> PersonalizationRule.of("ALWAYS", ""))
                .isInstanceOf(PersonalizationRule.InvalidPersonalizationRuleException.class);
        }
    }

    // =========================================================================
    // QrCode.configureRules()
    // =========================================================================

    @Nested
    @DisplayName("QrCode.configureRules()")
    class ConfigureRulesTest {

        @Test
        @DisplayName("configure geofence et personnalisation sur un QR actif")
        void configuresRulesOnActiveQr() {
            var qr = QrCode.create(owner, QrCodeType.PROFESSIONAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);

            var geofence = GeofenceRule.of(48.8566, 2.3522, 200);
            var personalization = PersonalizationRule.of("WORK_HOURS", "COLLEAGUE");

            qr.configureRules(geofence, personalization);

            assertThat(qr.hasGeofence()).isTrue();
            assertThat(qr.geofenceRule()).hasValue(geofence);
            assertThat(qr.personalizationRule()).hasValue(personalization);
        }

        @Test
        @DisplayName("configure avec null supprime les règles existantes")
        void configuringNullClearsRules() {
            var qr = QrCode.create(owner, QrCodeType.PROFESSIONAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);
            qr.configureRules(GeofenceRule.of(0.0, 0.0, 100), PersonalizationRule.alwaysForAnyone());

            // On efface les règles
            qr.configureRules(null, null);

            assertThat(qr.hasGeofence()).isFalse();
            assertThat(qr.geofenceRule()).isEmpty();
            assertThat(qr.personalizationRule()).isEmpty();
        }

        @Test
        @DisplayName("configure uniquement la géofence (personnalisation null)")
        void configuresGeofenceOnly() {
            var qr = QrCode.create(owner, QrCodeType.SOCIAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);

            qr.configureRules(GeofenceRule.of(5.3600, -4.0083, 500), null);

            assertThat(qr.hasGeofence()).isTrue();
            assertThat(qr.personalizationRule()).isEmpty();
        }

        @Test
        @DisplayName("rejette la configuration sur un QR révoqué")
        void rejectsConfigureOnRevokedQr() {
            var qr = QrCode.create(owner, QrCodeType.PROFESSIONAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);
            qr.revoke(clock);

            assertThatThrownBy(() -> qr.configureRules(
                    GeofenceRule.of(48.8566, 2.3522, 100), null
            )).isInstanceOf(QrCode.QrCodeRevokedException.class);
        }

        @Test
        @DisplayName("remplace des règles existantes")
        void replacesExistingRules() {
            var qr = QrCode.create(owner, QrCodeType.PROFESSIONAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);

            var firstGeofence  = GeofenceRule.of(48.8566, 2.3522, 100);
            var secondGeofence = GeofenceRule.of(5.3600, -4.0083, 250);

            qr.configureRules(firstGeofence, null);
            qr.configureRules(secondGeofence, PersonalizationRule.alwaysForAnyone());

            assertThat(qr.geofenceRule()).hasValue(secondGeofence);
        }
    }

    // =========================================================================
    // QrCode.reconstitute() — surcharge 11 paramètres
    // =========================================================================

    @Nested
    @DisplayName("QrCode.reconstitute() — avec GeofenceRule & PersonalizationRule")
    class ReconstitueWithRulesTest {

        @Test
        @DisplayName("reconstitue avec geofence et personnalisation")
        void reconstitueWithBothRules() {
            var id            = QrCodeId.generate();
            var geofence      = GeofenceRule.of(48.8566, 2.3522, 300);
            var personalization = PersonalizationRule.of("EVENING", "FRIEND");

            var qr = QrCode.reconstitute(
                    id, owner, QrCodeType.SOCIAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(),
                    NOW, 3, false, null,
                    geofence, personalization
            );

            assertThat(qr.id()).isEqualTo(id);
            assertThat(qr.hasGeofence()).isTrue();
            assertThat(qr.geofenceRule()).hasValue(geofence);
            assertThat(qr.personalizationRule()).hasValue(personalization);
            assertThat(qr.scanCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("reconstitue sans règles (null, null) — compatibilité ascendante")
        void reconstitueWithoutRules() {
            var qr = QrCode.reconstitute(
                    QrCodeId.generate(), owner, QrCodeType.PAYMENT,
                    ExpirationPolicy.permanent(), ScanLimit.of(5),
                    NOW, 0, false, null,
                    null, null
            );

            assertThat(qr.hasGeofence()).isFalse();
            assertThat(qr.geofenceRule()).isEmpty();
            assertThat(qr.personalizationRule()).isEmpty();
        }

        @Test
        @DisplayName("la surcharge 9 paramètres délègue à 11 avec null,null")
        void nineParamOverloadDelegatesToEleven() {
            var qr9  = QrCode.reconstitute(
                    QrCodeId.generate(), owner, QrCodeType.PROFESSIONAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(),
                    NOW, 2, false, null
            );

            assertThat(qr9.hasGeofence()).isFalse();
            assertThat(qr9.geofenceRule()).isEmpty();
            assertThat(qr9.personalizationRule()).isEmpty();
        }
    }
}
