package com.ssafy.e106.domain.subscription.dto.request;

import java.time.LocalDate;

public record SubscriptionUpdateRequest(
    Long servicePlanId,
    String bundleCode,
    LocalDate nextBillingDate) {
}
