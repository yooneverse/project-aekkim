package com.ssafy.e106.domain.subscription.dto.response;

import com.ssafy.e106.domain.promotion.dto.response.PromotionRecommendationSummary;
import com.ssafy.e106.domain.promotion.dto.response.PromotionRecommendationServiceResponse;
import com.ssafy.e106.domain.promotion.enums.PromotionType;
import java.util.List;

public record SubscriptionRecommendationResponse(
    Long promotionId,
    PromotionType promotionType,
    String title,
    String headline,
    Integer monthlySavingAmount,
    String billingCycle,
    String imageUrl,
    List<PromotionRecommendationServiceResponse> services) {

  public static SubscriptionRecommendationResponse from(PromotionRecommendationSummary summary) {
    return new SubscriptionRecommendationResponse(
        summary.promotionId(),
        summary.promotionType(),
        summary.title(),
        summary.headline(),
        summary.monthlySavingAmount(),
        summary.billingCycle(),
        summary.imageUrl(),
        summary.services());
  }
}
