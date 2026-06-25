package com.ssafy.e106.domain.finance.dto.request;

public record FinanceCreditCardTransactionListRequest(
    String cardNo,
    String cvc,
    String startDate,
    String endDate) {
}
