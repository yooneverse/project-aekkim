package com.ssafy.e106.domain.subscription.dto.response;

import com.ssafy.e106.domain.subscription.entity.Service;

public record ServiceListItemResponse(
    Long serviceId,
    String code,
    String name,
    String logoUrl) {

  public static ServiceListItemResponse of(Service service) {
    return new ServiceListItemResponse(
        service.getServiceId(),
        service.getCode(),
        service.getName(),
        service.getLogoUrl());
  }
}
