package com.ssafy.e106.domain.subscription.dto.response;

import java.util.List;

public record SubscriptionListResponse(
    Integer monthlyTotalAmount,
    List<SubscriptionListItemResponse> subscriptions) {
}
