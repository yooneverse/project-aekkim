package com.ssafy.e106.domain.payment.dto.response;

import java.util.List;

public record PaymentHistoryListResponse(
    int count,
    List<PaymentHistoryResponse> payments) {
}
