package com.uzima.infrastructure.persistence.qrcode;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataQrCodeRepository extends JpaRepository<QrCodeJpaEntity, UUID> {

    List<QrCodeJpaEntity> findByOwnerId(UUID ownerId);
}
