package com.ssafy.e106.domain.promotion.dto.response;

import com.ssafy.e106.domain.subscription.entity.Service;

public record PromotionDetailServiceResponse(
    Long serviceId,
    String code,
    String name,
    String logoUrl) {

  public static PromotionDetailServiceResponse from(Service service) {
    return new PromotionDetailServiceResponse(
        service.getServiceId(),
        service.getCode(),
        service.getName(),
        service.getLogoUrl());
  }
}
