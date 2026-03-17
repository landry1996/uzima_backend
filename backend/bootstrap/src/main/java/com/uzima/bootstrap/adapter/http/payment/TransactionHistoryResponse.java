package com.uzima.bootstrap.adapter.http.payment;

import com.uzima.application.payment.GetTransactionHistoryUseCase;

import java.util.List;

/**
 * DTO HTTP sortant : Historique paginé des transactions.
 */
public record TransactionHistoryResponse(
        List<PaymentResponse> sent,
        List<PaymentResponse> received,
        long totalSent,
        long totalReceived,
        long total
) {
    public static TransactionHistoryResponse from(GetTransactionHistoryUseCase.TransactionHistoryView view) {
        return new TransactionHistoryResponse(
            view.sent().stream().map(PaymentResponse::from).toList(),
            view.received().stream().map(PaymentResponse::from).toList(),
            view.totalSent(),
            view.totalReceived(),
            view.total()
        );
    }
}
