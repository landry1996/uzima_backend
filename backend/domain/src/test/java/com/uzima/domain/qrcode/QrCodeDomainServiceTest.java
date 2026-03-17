package com.uzima.domain.qrcode;

import com.uzima.domain.qrcode.model.*;
import com.uzima.domain.qrcode.port.QrCodeRepository;
import com.uzima.domain.qrcode.service.QrCodeDomainService;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitaires du service de domaine QrCodeDomainService.
 *
 * Appelle et vérifie :
 * - QrCodeDomainService.deleteIfOwned()        → QrCodeRepository.delete() + ownership check
 * - QrCodeDomainService.getLifecycleSummary()  → QrCode.createdAt() + ExpirationPolicy.expiresAt()
 * - LifecycleSummary.hasExpiration()           → Optional.isPresent() sur expiresAt()
 *
 * Pas de Mockito : QrCodeRepository implémenté in-memory pour rester dans les règles du module.
 */
class QrCodeDomainServiceTest {

    private static final Instant NOW    = Instant.parse("2026-03-12T10:00:00Z");
    private static final Instant FUTURE = NOW.plus(Duration.ofHours(48));

    private final TimeProvider clock = () -> NOW;

    private UserId owner;
    private UserId otherUser;
    private InMemoryQrCodeRepository repository;
    private QrCodeDomainService service;

    @BeforeEach
    void setUp() {
        owner     = UserId.generate();
        otherUser = UserId.generate();
        repository = new InMemoryQrCodeRepository();
        service    = new QrCodeDomainService(repository);
    }

    // -------------------------------------------------------------------------
    // deleteIfOwned()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("QrCodeDomainService.deleteIfOwned()")
    class DeleteIfOwnedTest {

        @Test
        @DisplayName("supprime le QR Code quand le demandeur en est le propriétaire")
        void deletesWhenOwnerMatches() {
            QrCode qr = QrCode.create(owner, QrCodeType.PROFESSIONAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);
            repository.save(qr);

            service.deleteIfOwned(qr.id(), owner);

            assertThat(repository.findById(qr.id())).isEmpty();
        }

        @Test
        @DisplayName("lève QrCodeOwnershipViolationException si l'utilisateur n'est pas propriétaire")
        void throwsWhenOwnershipViolated() {
            QrCode qr = QrCode.create(owner, QrCodeType.SOCIAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);
            repository.save(qr);

            assertThatThrownBy(() -> service.deleteIfOwned(qr.id(), otherUser))
                    .isInstanceOf(QrCodeDomainService.QrCodeOwnershipViolationException.class);

            // Le QR Code est toujours présent après l'échec
            assertThat(repository.findById(qr.id())).isPresent();
        }

        @Test
        @DisplayName("lève QrCodeNotFoundException si le QR Code n'existe pas")
        void throwsWhenQrCodeNotFound() {
            QrCodeId unknownId = QrCodeId.generate();

            assertThatThrownBy(() -> service.deleteIfOwned(unknownId, owner))
                    .isInstanceOf(QrCodeDomainService.QrCodeNotFoundException.class);
        }

        @Test
        @DisplayName("utilise QrCode.reconstitute() : le QR reconstitué conserve ses données")
        void reconstitutedQrCodePreservesOwnership() {
            // Simule ce que l'adaptateur infrastructure fait via QrCode.reconstitute()
            QrCodeId id = QrCodeId.generate();
            QrCode reconstituted = QrCode.reconstitute(
                    id, owner, QrCodeType.PAYMENT,
                    ExpirationPolicy.permanent(), ScanLimit.of(5),
                    NOW, 2, false, null
            );
            repository.save(reconstituted);

            // deleteIfOwned appelle findById() → reconstitute() dans l'adaptateur
            // puis vérifie ownerId() et appelle delete()
            assertThatNoException().isThrownBy(() -> service.deleteIfOwned(id, owner));
            assertThat(repository.findById(id)).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // getLifecycleSummary()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("QrCodeDomainService.getLifecycleSummary()")
    class GetLifecycleSummaryTest {

        @Test
        @DisplayName("retourne createdAt() du QR Code via le résumé")
        void summaryExposesCreatedAt() {
            QrCode qr = QrCode.create(owner, QrCodeType.PROFESSIONAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);
            repository.save(qr);

            QrCodeDomainService.LifecycleSummary summary = service.getLifecycleSummary(qr.id());

            // QrCode.createdAt() : horodatage fourni par TimeProvider au moment de la création
            assertThat(summary.createdAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("hasExpiration() retourne false pour une politique permanente — expiresAt() vide")
        void hasExpirationFalseForPermanent() {
            QrCode qr = QrCode.create(owner, QrCodeType.PROFESSIONAL,
                    ExpirationPolicy.permanent(), ScanLimit.unlimited(), clock);
            repository.save(qr);

            QrCodeDomainService.LifecycleSummary summary = service.getLifecycleSummary(qr.id());

            // ExpirationPolicy.expiresAt() → Optional.empty() pour une politique permanente
            assertThat(summary.expiresAt()).isEmpty();
            assertThat(summary.hasExpiration()).isFalse();
        }

        @Test
        @DisplayName("hasExpiration() retourne true et expiresAt() présente pour une politique temporelle")
        void hasExpirationTrueForTimed() {
            ExpirationPolicy policy = ExpirationPolicy.expiresAt(FUTURE, NOW);
            QrCode qr = QrCode.create(owner, QrCodeType.EVENT, policy, ScanLimit.unlimited(), clock);
            repository.save(qr);

            QrCodeDomainService.LifecycleSummary summary = service.getLifecycleSummary(qr.id());

            // ExpirationPolicy.expiresAt() → Optional<Instant> avec la date d'expiration
            assertThat(summary.expiresAt()).hasValue(FUTURE);
            assertThat(summary.hasExpiration()).isTrue();
        }

        @Test
        @DisplayName("lève QrCodeNotFoundException si le QR Code est inconnu")
        void throwsWhenNotFound() {
            assertThatThrownBy(() -> service.getLifecycleSummary(QrCodeId.generate()))
                    .isInstanceOf(QrCodeDomainService.QrCodeNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Fausse implémentation in-memory de QrCodeRepository
    // -------------------------------------------------------------------------

    /**
     * Implémentation in-memory de QrCodeRepository pour les tests du module domain.
     * Simule le comportement de l'adaptateur infrastructure sans dépendance externe.
     * findById() retourne un QrCode reconstitué via QrCode.reconstitute() dans les cas réels.
     */
    static class InMemoryQrCodeRepository implements QrCodeRepository {

        private final Map<QrCodeId, QrCode> store = new LinkedHashMap<>();

        @Override
        public void save(QrCode qrCode) {
            store.put(qrCode.id(), qrCode);
        }

        @Override
        public Optional<QrCode> findById(QrCodeId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<QrCode> findByOwnerId(UserId ownerId) {
            return store.values().stream()
                    .filter(qr -> qr.ownerId().equals(ownerId))
                    .toList();
        }

        @Override
        public void delete(QrCodeId id) {
            store.remove(id);
        }
    }
}
