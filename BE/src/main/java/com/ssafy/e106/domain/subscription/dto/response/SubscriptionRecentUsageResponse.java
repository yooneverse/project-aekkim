package com.ssafy.e106.domain.subscription.dto.response;

import com.ssafy.e106.domain.subscription.entity.CheckinRecord;

public record SubscriptionRecentUsageResponse(
    String cycleYm,
    String response,
    Long serviceId,
    String serviceName) {

  public static SubscriptionRecentUsageResponse from(CheckinRecord checkinRecord) {
    return new SubscriptionRecentUsageResponse(
        checkinRecord.getCycleYm(),
        checkinRecord.getResponse().name(),
        checkinRecord.getService().getServiceId(),
        checkinRecord.getService().getName());
  }
}
