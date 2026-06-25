package com.ssafy.e106.domain.finance.dto.request;

public record FinanceMerchantCreateRequest(
    String categoryId,
    String merchantName) {
}
