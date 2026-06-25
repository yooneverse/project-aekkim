package com.ssafy.e106.domain.admin.dto.request;

import java.util.List;

public record SubscriptionUsageSeedPayload(
    List<SubscriptionUsageSeedItem> items
) {
}
