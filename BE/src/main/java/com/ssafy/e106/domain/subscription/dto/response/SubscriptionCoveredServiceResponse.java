package com.ssafy.e106.domain.subscription.dto.response;

import com.ssafy.e106.domain.subscription.entity.Service;

public record SubscriptionCoveredServiceResponse(
    Long serviceId,
    String serviceCode,
    String serviceName,
    String logoUrl) {

  public static SubscriptionCoveredServiceResponse from(Service service) {
    return new SubscriptionCoveredServiceResponse(
        service.getServiceId(),
        service.getCode(),
        service.getName(),
        service.getLogoUrl());
  }
}
