package com.ssafy.e106.domain.subscriptionusage.dto.response;

import java.time.LocalDate;

public record SubscriptionUsageReportItemResponse(
    Long subscriptionId,
    String subscriptionType,
    Long serviceId,
    String bundleCode,
    String serviceName,
    String planName,
    String category,
    String logoUrl,
    Integer monthlyPrice,
    Integer totalUsedMinutes,
    Integer usedDays,
    LocalDate lastUsedDate,
    Integer hourlyCost,
    String nudgeMessage) {
}
