package com.ssafy.e106.domain.admin.dto.response;

import java.time.LocalDate;

public record AdminSubscriptionUsageViewResponse(
    Long usageId,
    Long userId,
    Long serviceId,
    String serviceCode,
    String serviceName,
    LocalDate usageDate,
    Integer usedMinutes
) {
}
