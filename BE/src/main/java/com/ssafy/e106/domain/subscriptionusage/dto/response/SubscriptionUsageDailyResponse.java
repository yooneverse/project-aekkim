package com.ssafy.e106.domain.subscriptionusage.dto.response;

import java.util.List;

public record SubscriptionUsageDailyResponse(
    Integer days,
    Long subscriptionId,
    List<SubscriptionUsageDailyPointResponse> items) {
}
