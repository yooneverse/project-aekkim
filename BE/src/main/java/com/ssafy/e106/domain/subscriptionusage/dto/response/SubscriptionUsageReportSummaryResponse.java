package com.ssafy.e106.domain.subscriptionusage.dto.response;

public record SubscriptionUsageReportSummaryResponse(
    Integer windowDays,
    Integer totalUsedMinutes,
    Integer activeSubscriptionCount,
    Integer lowUsageSubscriptionCount,
    String mostUsedSubscriptionName,
    Integer mostUsedSubscriptionMinutes) {
}
