package com.uzima.domain.qrcode.specification;

import com.uzima.domain.qrcode.model.QrCode;
import com.uzima.domain.shared.specification.Specification;
import com.uzima.domain.user.model.UserId;

import java.util.Objects;

/**
 * Specification : vérifie qu'un QR Code appartient à l'utilisateur donné.
 * Justification de son existence (au-delà du simple getter) :
 * Combinée avec QrCodeIsActiveSpecification via .and(), elle permet d'exprimer
 * des règles composées en une seule ligne dans les use cases :
 *
 * <pre>
 *   Specification&lt;QrCode&gt; canRevoke =
 *       new QrCodeBelongsToOwnerSpecification(requesterId)
 *           .and(new QrCodeIsActiveSpecification(now).not()); // ne révoque que les QR actifs
 *
 *   Specification&lt;QrCode&gt; isAccessible =
 *       new QrCodeIsActiveSpecification(now)
 *           .and(new QrCodeBelongsToOwnerSpecification(userId)
 *               .or(isSharedWith(userId)));
 * </pre>
 *
 * Sans cette specification, chaque use case dupliquerait :
 *   if (!qrCode.ownerId().equals(requesterId)) throw…
 * rendant difficile la composition avec d'autres règles.
 */
public final class QrCodeBelongsToOwnerSpecification implements Specification<QrCode> {

    private final UserId ownerId;

    public QrCodeBelongsToOwnerSpecification(UserId ownerId) {
        this.ownerId = Objects.requireNonNull(ownerId, "L'identifiant du propriétaire est obligatoire");
    }

    @Override
    public boolean isSatisfiedBy(QrCode qrCode) {
        Objects.requireNonNull(qrCode, "Le QR Code ne peut pas être nul");
        return ownerId.equals(qrCode.ownerId());
    }
}
