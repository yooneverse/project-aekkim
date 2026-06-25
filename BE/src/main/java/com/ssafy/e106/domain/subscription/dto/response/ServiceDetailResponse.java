package com.ssafy.e106.domain.subscription.dto.response;

import java.util.List;

import com.ssafy.e106.domain.subscription.entity.Service;

public record ServiceDetailResponse(
    Long serviceId,
    String code,
    String name,
    String category,
    String logoUrl,
    List<ServicePlanResponse> plans) {

  public static ServiceDetailResponse of(Service service, List<ServicePlanResponse> plans) {
    return new ServiceDetailResponse(
        service.getServiceId(),
        service.getCode(),
        service.getName(),
        service.getCategory().name(),
        service.getLogoUrl(),
        plans);
  }
}
