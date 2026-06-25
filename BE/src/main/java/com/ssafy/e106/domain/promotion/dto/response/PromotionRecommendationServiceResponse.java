package com.ssafy.e106.domain.promotion.dto.response;

import com.ssafy.e106.domain.promotion.entity.PromotionServiceMapping;
import com.ssafy.e106.domain.subscription.entity.Service;

public record PromotionRecommendationServiceResponse(
    Long serviceId,
    String serviceName,
    String logoUrl) {

  public static PromotionRecommendationServiceResponse from(PromotionServiceMapping mapping) {
    return from(mapping.getService());
  }

  public static PromotionRecommendationServiceResponse from(Service service) {
    return new PromotionRecommendationServiceResponse(
        service.getServiceId(),
        service.getName(),
        service.getLogoUrl());
  }
}
