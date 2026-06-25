package com.ssafy.e106.domain.subscriptionusage.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record SubscriptionUsageDailyUpsertRequest(
    @NotEmpty(message = "items는 최소 1건 이상 필요합니다.")
    @Size(max = 200, message = "items는 최대 200건까지 처리할 수 있습니다.")
    List<@Valid SubscriptionUsageDailyItemRequest> items) {
}
