package com.ssafy.e106.domain.finance.dto.request;

public record FinanceCreditCardTransactionCreateRequest(
    String cardNo,
    String cvc,
    String merchantId,
    String paymentBalance) {
}
