package com.uzima.infrastructure.persistence.qrcode;

import com.uzima.application.qrcode.port.out.QrCodeRepositoryPort;
import com.uzima.domain.qrcode.model.QrCode;
import com.uzima.domain.qrcode.model.QrCodeId;
import com.uzima.domain.user.model.UserId;

import java.util.List;
import java.util.Optional;

public class QrCodeRepositoryAdapter implements QrCodeRepositoryPort {

    private final SpringDataQrCodeRepository jpa;
    private final QrCodeEntityMapper         mapper = new QrCodeEntityMapper();

    public QrCodeRepositoryAdapter(SpringDataQrCodeRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(QrCode qrCode) {
        jpa.save(mapper.toJpaEntity(qrCode));
    }

    @Override
    public Optional<QrCode> findById(QrCodeId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<QrCode> findByOwnerId(UserId ownerId) {
        return jpa.findByOwnerId(ownerId.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(QrCodeId id) {
        jpa.deleteById(id.value());
    }
}
