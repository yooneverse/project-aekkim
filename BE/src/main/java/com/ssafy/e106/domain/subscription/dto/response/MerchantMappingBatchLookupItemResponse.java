package com.ssafy.e106.domain.subscription.dto.response;

public record MerchantMappingBatchLookupItemResponse(
    String merchantRaw,
    boolean matched,
    Long serviceId,
    Long servicePlanId,
    Integer hitCount) {
}
