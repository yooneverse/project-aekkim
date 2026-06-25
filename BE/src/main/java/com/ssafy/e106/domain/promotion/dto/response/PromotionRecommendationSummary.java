package com.ssafy.e106.domain.promotion.dto.response;

import com.ssafy.e106.domain.promotion.entity.Promotion;
import com.ssafy.e106.domain.promotion.enums.PromotionType;
import com.ssafy.e106.domain.subscription.entity.Bundle;
import java.util.List;

public record PromotionRecommendationSummary(
    Long promotionId,
    PromotionType promotionType,
    String title,
    String headline,
    Integer monthlySavingAmount,
    String billingCycle,
    String imageUrl,
    List<PromotionRecommendationServiceResponse> services) {

  public static PromotionRecommendationSummary of(
      Promotion promotion,
      Integer monthlySavingAmount,
      List<PromotionRecommendationServiceResponse> services) {
    return new PromotionRecommendationSummary(
        promotion.getPromotionId(),
        promotion.getPromotionType(),
        promotion.getTitle(),
        buildHeadline(promotion.getPromotionType(), promotion.getTitle(), promotion.getTitle()),
        monthlySavingAmount,
        promotion.getBillingCycle(),
        promotion.getImageUrl(),
        services);
  }

  public static PromotionRecommendationSummary ofBundle(
      Bundle bundle,
      Integer monthlySavingAmount,
      List<PromotionRecommendationServiceResponse> services) {
    return new PromotionRecommendationSummary(
        -bundle.getBundleId(),
        PromotionType.BUNDLE,
        bundle.getName(),
        buildHeadline(PromotionType.BUNDLE, bundle.getName(), bundle.getPlanName()),
        monthlySavingAmount,
        bundle.getBillingCycle(),
        bundle.getLogoUrl(),
        services);
  }

  private static String buildHeadline(
      PromotionType promotionType,
      String title,
      String fallbackHeadline) {
    if (promotionType == PromotionType.CARD_BENEFIT) {
      return title + " 카드 혜택을 확인해보세요";
    }
    if (promotionType == PromotionType.BUNDLE) {
      return fallbackHeadline;
    }
    return title;
  }
}
