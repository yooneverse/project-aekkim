package com.ssafy.e106.domain.promotion.dto.response;

import java.time.LocalDateTime;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import com.ssafy.e106.domain.promotion.entity.Promotion;
import com.ssafy.e106.domain.promotion.enums.PromotionType;
import com.ssafy.e106.domain.promotion.service.PromotionRecommendationEvaluation;
import com.ssafy.e106.domain.subscription.entity.Bundle;

public record PromotionRecommendationItemResponse(
    Long promotionId,
    String promotionType,
    String title,
    String summary,
    Integer originalPrice,
    Integer discountPrice,
    String billingCycle,
    String sourceUrl,
    String imageUrl,
    Integer recommendationScore,
    List<String> recommendationReasons,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    List<PromotionRecommendationServiceResponse> services) {

  public static PromotionRecommendationItemResponse of(
      Promotion promotion,
      PromotionRecommendationEvaluation evaluation,
      List<PromotionRecommendationServiceResponse> services) {
    return new PromotionRecommendationItemResponse(
        promotion.getPromotionId(),
        promotion.getPromotionType().name(),
        promotion.getTitle(),
        promotion.getSummary(),
        promotion.getOriginalPrice(),
        promotion.getDiscountPrice(),
        promotion.getBillingCycle(),
        promotion.getSourceUrl(),
        promotion.getImageUrl(),
        evaluation.score(),
        evaluation.reasonLabels(),
        promotion.getStartsAt(),
        promotion.getEndsAt(),
        services);
  }

  public static PromotionRecommendationItemResponse ofBundle(
      Bundle bundle,
      PromotionRecommendationEvaluation evaluation,
      List<PromotionRecommendationServiceResponse> services,
      LocalDateTime startsAt,
      LocalDateTime endsAt) {
    return new PromotionRecommendationItemResponse(
        -bundle.getBundleId(),
        PromotionType.BUNDLE.name(),
        bundle.getName(),
        buildBundleSummary(bundle, services),
        bundle.getOriginalPrice(),
        bundle.getMonthlyPrice(),
        bundle.getBillingCycle(),
        bundle.getSourceUrl(),
        bundle.getLogoUrl(),
        evaluation.score(),
        evaluation.reasonLabels(),
        startsAt,
        endsAt,
        services);
  }

  private static String buildBundleSummary(
      Bundle bundle,
      List<PromotionRecommendationServiceResponse> services) {
    String coveredServices = services.stream()
        .map(PromotionRecommendationServiceResponse::serviceName)
        .toList()
        .stream()
        .reduce((left, right) -> left + ", " + right)
        .orElse(bundle.getPlanName());

    Integer monthlySavingAmount = calculateMonthlySavingAmount(bundle);
    if (monthlySavingAmount != null && monthlySavingAmount > 0) {
      return coveredServices + " 조합을 묶어 월 "
          + formatWon(monthlySavingAmount)
          + " 절약할 수 있는 번들입니다.";
    }
    return coveredServices + " 조합을 한 번에 관리할 수 있는 번들입니다.";
  }

  private static Integer calculateMonthlySavingAmount(Bundle bundle) {
    if (bundle.getOriginalPrice() == null || bundle.getMonthlyPrice() == null) {
      return null;
    }
    return bundle.getOriginalPrice() - bundle.getMonthlyPrice();
  }

  private static String formatWon(Integer amount) {
    return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원";
  }
}
