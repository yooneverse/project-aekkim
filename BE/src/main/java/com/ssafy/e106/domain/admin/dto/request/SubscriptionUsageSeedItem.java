package com.ssafy.e106.domain.admin.dto.request;

import java.time.LocalDate;

public record SubscriptionUsageSeedItem(
    String serviceCode,
    LocalDate usageDate,
    Integer usedMinutes
) {
}
