package com.uzima.domain.qrcode;

import com.uzima.domain.qrcode.factory.QrCodeFactory;
import com.uzima.domain.qrcode.model.*;
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
 * Tests unitaires de QrCodeFactory.
 * Vérifie que la factory applique correctement les règles par type.
 */
class QrCodeFactoryTest {

    private static final Instant NOW = Instant.parse("2026-03-12T10:00:00Z");
    private final TimeProvider clock = () -> NOW;
    private UserId owner;

    @BeforeEach
    void setUp() {
        owner = UserId.generate();
    }

    @Test
    @DisplayName("createProfessional : permanent, illimité")
    void professionalIsPermanentAndUnlimited() {
        QrCode qr = QrCodeFactory.createProfessional(owner, clock);
        assertThat(qr.type()).isEqualTo(QrCodeType.PROFESSIONAL);
        assertThat(qr.expirationPolicy().isPermanent()).isTrue();
        assertThat(qr.scanLimit().isUnlimited()).isTrue();
        assertThat(qr.isActiveAt(NOW)).isTrue();
    }

    @Test
    @DisplayName("createPayment singleUse : 1 scan max")
    void paymentSingleUseHasOneScanLimit() {
        QrCode qr = QrCodeFactory.createPayment(owner, true, clock);
        assertThat(qr.type()).isEqualTo(QrCodeType.PAYMENT);
        assertThat(qr.scanLimit().maxScans()).hasValue(1);
    }

    @Test
    @DisplayName("createPayment multi : scans illimités")
    void paymentMultiUseIsUnlimited() {
        QrCode qr = QrCodeFactory.createPayment(owner, false, clock);
        assertThat(qr.scanLimit().isUnlimited()).isTrue();
    }

    @Test
    @DisplayName("createTemporaryLocation : expire après la durée spécifiée")
    void temporaryLocationExpiresAfterDuration() {
        Duration validity = Duration.ofHours(2);
        QrCode qr = QrCodeFactory.createTemporaryLocation(owner, validity, clock);

        assertThat(qr.type()).isEqualTo(QrCodeType.TEMPORARY_LOCATION);
        assertThat(qr.expirationPolicy().isPermanent()).isFalse();
        assertThat(qr.isActiveAt(NOW.plus(Duration.ofHours(1)))).isTrue();
        assertThat(qr.isActiveAt(NOW.plus(Duration.ofHours(3)))).isFalse();
    }

    @Test
    @DisplayName("createTemporaryLocation : rejette une durée < 30 minutes")
    void temporaryLocationRejectsTooShortDuration() {
        assertThatThrownBy(() -> QrCodeFactory.createTemporaryLocation(owner, Duration.ofMinutes(10), clock))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("30 minutes");
    }

    @Test
    @DisplayName("createTemporaryLocation : rejette une durée > 48 heures")
    void temporaryLocationRejectsTooLongDuration() {
        assertThatThrownBy(() -> QrCodeFactory.createTemporaryLocation(owner, Duration.ofDays(3), clock))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("48 heures");
    }

    @Test
    @DisplayName("createMedicalEmergency : permanent, illimité, offline")
    void medicalEmergencyIsPermanentAndUnlimited() {
        QrCode qr = QrCodeFactory.createMedicalEmergency(owner, clock);
        assertThat(qr.type()).isEqualTo(QrCodeType.MEDICAL_EMERGENCY);
        assertThat(qr.type().requiresOfflineSupport()).isTrue();
        assertThat(qr.expirationPolicy().isPermanent()).isTrue();
        assertThat(qr.scanLimit().isUnlimited()).isTrue();
    }

    @Nested
    @DisplayName("Specification QrCodeIsActive")
    class SpecificationTest {

        @Test
        @DisplayName("active QR satisfait la spécification")
        void activeQrSatisfiesSpec() {
            var qr = QrCodeFactory.createProfessional(owner, clock);
            var spec = new com.uzima.domain.qrcode.specification.QrCodeIsActiveSpecification(NOW);
            assertThat(spec.isSatisfiedBy(qr)).isTrue();
        }

        @Test
        @DisplayName("QR révoqué ne satisfait pas la spécification")
        void revokedQrDoesNotSatisfySpec() {
            var qr = QrCodeFactory.createProfessional(owner, clock);
            qr.revoke(clock);
            var spec = new com.uzima.domain.qrcode.specification.QrCodeIsActiveSpecification(NOW);
            assertThat(spec.isSatisfiedBy(qr)).isFalse();
            assertThat(spec.failureReason(qr)).isEqualTo("QR_CODE_REVOKED");
        }

        @Test
        @DisplayName("composition de specifications avec and()")
        void specificationComposition() {
            var qr = QrCodeFactory.createProfessional(owner, clock);
            var activeSpec = new com.uzima.domain.qrcode.specification.QrCodeIsActiveSpecification(NOW);
            // Composition : actif ET appartient au bon propriétaire
            var ownerSpec = (com.uzima.domain.shared.specification.Specification<com.uzima.domain.qrcode.model.QrCode>)
                    candidate -> candidate.ownerId().equals(owner);
            var combined = activeSpec.and(ownerSpec);
            assertThat(combined.isSatisfiedBy(qr)).isTrue();
        }
    }
}
