package com.ssafy.e106.domain.payment.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.ssafy.e106.domain.payment.entity.PaymentHistory;

public record PaymentHistoryResponse(
    Long paymentHistoryId,
    String paymentId,
    String merchantRaw,
    Integer amount,
    LocalDate paymentDate,
    String category,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static PaymentHistoryResponse of(PaymentHistory paymentHistory) {
    return new PaymentHistoryResponse(
        paymentHistory.getPaymentHistoryId(),
        paymentHistory.getPaymentId(),
        paymentHistory.getMerchantRaw(),
        paymentHistory.getAmount(),
        paymentHistory.getPaymentDate(),
        paymentHistory.getCategory(),
        paymentHistory.getCreatedAt(),
        paymentHistory.getUpdatedAt());
  }
}
