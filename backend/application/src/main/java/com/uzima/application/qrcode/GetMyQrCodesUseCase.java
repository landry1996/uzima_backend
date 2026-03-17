package com.uzima.application.qrcode;

import com.uzima.application.qrcode.port.out.QrCodeRepositoryPort;
import com.uzima.domain.qrcode.model.QrCode;
import com.uzima.domain.qrcode.specification.QrCodeBelongsToOwnerSpecification;
import com.uzima.domain.qrcode.specification.QrCodeIsActiveSpecification;
import com.uzima.domain.qrcode.model.ExpirationPolicy;
import com.uzima.domain.shared.TimeProvider;
import com.uzima.domain.user.model.UserId;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Use Case (Query) : Récupérer les QR Codes d'un utilisateur.
 * <p>
 * Justifie l'existence de :
 * - QrCodeRepository.findByOwnerId()
 * - QrCodeBelongsToOwnerSpecification (double vérification défensive)
 * - Specification.and() (composition des deux specs)
 * - QrCode.createdAt() (accès dans la projection)
 * - ExpirationPolicy.expiresOnDate() + TimeProvider.today()
 *   → signale les QR codes qui expirent aujourd'hui
 * <p>
 * Pas de Spring. Pas de framework.
 */
public final class GetMyQrCodesUseCase {

    private final QrCodeRepositoryPort qrCodeRepository;
    private final TimeProvider clock;

    public GetMyQrCodesUseCase(QrCodeRepositoryPort qrCodeRepository, TimeProvider clock) {
        this.qrCodeRepository = Objects.requireNonNull(qrCodeRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Vue d'un QR Code enrichie de métadonnées calculées.
     *
     * @param qrCode          Le QR Code
     * @param isActive        Actif au moment de la requête (non révoqué, non expiré, limite non atteinte)
     * @param expiringToday   Vrai si le QR Code expire aujourd'hui (alerte utilisateur)
     */
    public record QrCodeView(QrCode qrCode, boolean isActive, boolean expiringToday) {}

    /**
     * Récupère tous les QR Codes de l'utilisateur avec leur statut calculé.
     *
     * @param userId Identifiant du propriétaire
     * @return La liste des QR Codes avec leur statut en temps réel
     */
    public List<QrCodeView> execute(UserId userId) {
        Objects.requireNonNull(userId, "L'identifiant utilisateur est obligatoire");

        // findByOwnerId justifié ici (réponse à "voir mes QR codes")
        List<QrCode> qrCodes = qrCodeRepository.findByOwnerId(userId);

        // Vérification défensive : QrCodeBelongsToOwnerSpecification
        // Composed with QrCodeIsActiveSpecification via .and() — justifie Specification.and()
        var ownerSpec = new QrCodeBelongsToOwnerSpecification(userId);
        var activeSpec = new QrCodeIsActiveSpecification(clock.now());
        var ownedAndActive = ownerSpec.and(activeSpec); // .and() justifié ici

        LocalDate today = clock.today(); // .today() justifié ici

        return qrCodes.stream()
                .filter(ownerSpec::isSatisfiedBy) // double vérification défensive
                .map(qrCode -> {
                    boolean isActive = ownedAndActive.isSatisfiedBy(qrCode);
                    // expiresOnDate justifié ici — alerte "expire aujourd'hui"
                    boolean expiringToday = qrCode.expirationPolicy().expiresOnDate(today);
                    return new QrCodeView(qrCode, isActive, expiringToday);
                })
                .toList();
    }
}
