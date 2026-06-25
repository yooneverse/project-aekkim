package com.ssafy.e106.domain.subscription.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MerchantMappingBatchLookupItemRequest(
    @NotBlank(message = "merchantRaw는 비어 있을 수 없습니다.")
    String merchantRaw,
    @NotNull(message = "predictedServiceId는 필수입니다.")
    Long predictedServiceId,
    @NotNull(message = "predictedServicePlanId는 필수입니다.")
    Long predictedServicePlanId) {
}
