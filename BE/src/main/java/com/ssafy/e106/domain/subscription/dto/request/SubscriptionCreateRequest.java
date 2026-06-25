package com.ssafy.e106.domain.subscription.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionCreateRequest(
    @NotBlank(message = "subscriptionType은 필수입니다.")
    String subscriptionType,
    Long serviceId,
    Long servicePlanId,
    String bundleCode,
    LocalDate nextBillingDate) {
}
