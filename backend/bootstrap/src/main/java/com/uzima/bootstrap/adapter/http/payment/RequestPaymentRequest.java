package com.uzima.bootstrap.adapter.http.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO HTTP entrant : Demande de paiement (request money).
 */
public record RequestPaymentRequest(
        @NotBlank String debtorId,
        @NotNull @DecimalMin(value = "0.01", message = "Le montant doit être positif") BigDecimal amount,
        @NotBlank String currency,
        @NotBlank String preferredMethod,
        String reason
) {}
