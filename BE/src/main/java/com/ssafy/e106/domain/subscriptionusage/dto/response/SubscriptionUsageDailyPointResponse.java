package com.ssafy.e106.domain.subscriptionusage.dto.response;

import java.time.LocalDate;

import com.ssafy.e106.domain.subscription.enums.ServiceCategory;

public record SubscriptionUsageDailyPointResponse(
    LocalDate usageDate,
    Integer totalUsedMinutes,
    Integer ottUsedMinutes,
    Integer musicUsedMinutes,
    Integer aiUsedMinutes,
    ServiceCategory dominantCategory) {
}
