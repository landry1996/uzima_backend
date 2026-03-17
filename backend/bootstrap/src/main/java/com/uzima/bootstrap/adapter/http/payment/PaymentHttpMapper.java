package com.uzima.bootstrap.adapter.http.payment;

import com.uzima.application.payment.port.in.RequestPaymentCommand;
import com.uzima.application.payment.port.in.SendPaymentCommand;
import com.uzima.domain.payment.model.Currency;
import com.uzima.domain.payment.model.PaymentMethod;
import com.uzima.domain.user.model.UserId;

/**
 * Mapper HTTP ↔ Application : Paiements.
 * <p>
 * Convertit les DTOs HTTP en commandes application.
 * La validation des enums (Currency, PaymentMethod) lève IllegalArgumentException
 * → traduite en 400 Bad Request par le GlobalExceptionHandler.
 */
public final class PaymentHttpMapper {

    private PaymentHttpMapper() {}

    public static SendPaymentCommand toSendCommand(SendPaymentRequest request, UserId senderId) {
        return new SendPaymentCommand(
            senderId,
            UserId.of(request.recipientId()),
            request.amount(),
            Currency.valueOf(request.currency().toUpperCase()),
            PaymentMethod.valueOf(request.method().toUpperCase()),
            request.description()
        );
    }

    public static RequestPaymentCommand toRequestCommand(RequestPaymentRequest request, UserId requesterId) {
        return new RequestPaymentCommand(
            requesterId,
            UserId.of(request.debtorId()),
            request.amount(),
            Currency.valueOf(request.currency().toUpperCase()),
            PaymentMethod.valueOf(request.preferredMethod().toUpperCase()),
            request.reason()
        );
    }
}
