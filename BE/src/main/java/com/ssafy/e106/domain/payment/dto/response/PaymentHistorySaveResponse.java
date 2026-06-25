package com.ssafy.e106.domain.payment.dto.response;

import java.util.List;

public record PaymentHistorySaveResponse(
    int requestedCount,
    int savedCount,
    int skippedCount,
    List<PaymentHistoryResponse> savedPayments) {
}
