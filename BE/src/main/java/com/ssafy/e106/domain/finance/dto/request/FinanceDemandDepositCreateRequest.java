package com.ssafy.e106.domain.finance.dto.request;

public record FinanceDemandDepositCreateRequest(
    String bankCode,
    String accountName,
    String accountDescription) {
}
