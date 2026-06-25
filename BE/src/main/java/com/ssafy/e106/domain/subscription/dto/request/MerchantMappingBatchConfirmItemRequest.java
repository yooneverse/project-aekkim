package com.ssafy.e106.domain.subscription.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MerchantMappingBatchConfirmItemRequest(
    @NotBlank(message = "merchantRaw는 필수입니다.") String merchantRaw,
    @NotNull(message = "serviceId는 필수입니다.") Long serviceId,
    @NotNull(message = "servicePlanId는 필수입니다.") Long servicePlanId,
    LocalDate nextBillingDate) {
}
