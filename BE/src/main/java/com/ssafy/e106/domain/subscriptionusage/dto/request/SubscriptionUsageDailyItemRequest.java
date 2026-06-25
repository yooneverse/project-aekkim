package com.ssafy.e106.domain.subscriptionusage.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubscriptionUsageDailyItemRequest(
    @NotNull(message = "serviceId는 필수입니다.")
    Long serviceId,

    @NotNull(message = "usageDate는 필수입니다.")
    LocalDate usageDate,

    @NotNull(message = "usedMinutes는 필수입니다.")
    @Min(value = 0, message = "usedMinutes는 0 이상이어야 합니다.")
    Integer usedMinutes) {
}
