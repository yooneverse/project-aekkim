package com.ssafy.e106.domain.admin.dto.response;

import java.time.LocalDate;

public record AdminSubscriptionViewResponse(
    Long subscriptionId,
    Long userId,
    String subscriptionType,
    Long serviceId,
    String serviceCode,
    String serviceName,
    Long servicePlanId,
    String servicePlanName,
    String billingCycle,
    Long bundleId,
    String bundleCode,
    String bundleName,
    LocalDate nextBillingDate
) {
}
