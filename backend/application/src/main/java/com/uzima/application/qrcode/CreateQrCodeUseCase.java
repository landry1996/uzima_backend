package com.uzima.application.qrcode;

import com.uzima.application.qrcode.port.in.CreateQrCodeCommand;
import com.uzima.application.qrcode.port.out.QrCodeRepositoryPort;
import com.uzima.domain.qrcode.factory.QrCodeFactory;
import com.uzima.domain.qrcode.model.QrCode;
import com.uzima.domain.shared.TimeProvider;

import java.util.Objects;

/**
 * Use Case : Création d'un QR Code contextuel.
 * <p>
 * Justifie l'existence de :
 * - QrCodeFactory.createSocial()
 * - QrCodeFactory.createEvent()
 * - QrCodeRepository (et QrCodeRepositoryPort)
 * <p>
 * La factory est le seul point de création : les règles par type
 * (expiration obligatoire pour EVENT, illimité pour SOCIAL, etc.)
 * sont encapsulées dans QrCodeFactory et ne fuient pas dans ce use case.
 * <p>
 * Pas de Spring. Pas de framework.
 */
public final class CreateQrCodeUseCase {

    private final QrCodeRepositoryPort qrCodeRepository;
    private final TimeProvider clock;

    public CreateQrCodeUseCase(QrCodeRepositoryPort qrCodeRepository, TimeProvider clock) {
        this.qrCodeRepository = Objects.requireNonNull(qrCodeRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Crée et persiste un QR Code selon le type demandé.
     *
     * @param command La commande contenant le type et les paramètres optionnels
     * @return Le QR Code créé
     * @throws IllegalArgumentException si les paramètres sont invalides pour le type
     */
    public QrCode execute(CreateQrCodeCommand command) {
        Objects.requireNonNull(command, "La commande ne peut pas être nulle");

        QrCode qrCode = switch (command.type()) {
            case PROFESSIONAL -> QrCodeFactory.createProfessional(command.ownerId(), clock);
            // createSocial justifié ici — Nouveauté QR Code social du super-app
            case SOCIAL -> QrCodeFactory.createSocial(command.ownerId(), clock);
            case PAYMENT -> QrCodeFactory.createPayment(command.ownerId(), command.singleUsePayment(), clock);
            case TEMPORARY_LOCATION -> {
                if (command.validFor() == null) {
                    throw new IllegalArgumentException(
                        "Une durée de validité est obligatoire pour un QR Code de localisation temporaire"
                    );
                }
                yield QrCodeFactory.createTemporaryLocation(command.ownerId(), command.validFor(), clock);
            }
            // createEvent justifié ici — Nouveauté QR Code événement
            case EVENT -> {
                if (command.validFor() == null) {
                    throw new IllegalArgumentException(
                        "Une durée de validité est obligatoire pour un QR Code d'événement"
                    );
                }
                yield QrCodeFactory.createEvent(command.ownerId(), command.validFor(), clock);
            }
            case MEDICAL_EMERGENCY -> QrCodeFactory.createMedicalEmergency(command.ownerId(), clock);
        };

        qrCodeRepository.save(qrCode);
        return qrCode;
    }
}
