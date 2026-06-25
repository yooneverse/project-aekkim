package com.ssafy.e106.domain.promotion.dto.response;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.ssafy.e106.domain.promotion.entity.Promotion;
import com.ssafy.e106.domain.promotion.enums.PromotionType;
import com.ssafy.e106.domain.subscription.entity.Bundle;

public record PromotionDetailResponse(
    Long promotionId,
    String promotionType,
    String title,
    String summary,
    Integer originalPrice,
    Integer discountPrice,
    String billingCycle,
    String sourceUrl,
    String imageUrl,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    List<PromotionDetailServiceResponse> services) {

  public static PromotionDetailResponse of(
      Promotion promotion,
      List<PromotionDetailServiceResponse> services) {
    return new PromotionDetailResponse(
        promotion.getPromotionId(),
        promotion.getPromotionType().name(),
        promotion.getTitle(),
        promotion.getSummary(),
        promotion.getOriginalPrice(),
        promotion.getDiscountPrice(),
        promotion.getBillingCycle(),
        promotion.getSourceUrl(),
        promotion.getImageUrl(),
        promotion.getStartsAt(),
        promotion.getEndsAt(),
        services);
  }

  public static PromotionDetailResponse ofBundle(
      Bundle bundle,
      List<PromotionDetailServiceResponse> services,
      LocalDateTime startsAt,
      LocalDateTime endsAt) {
    return new PromotionDetailResponse(
        -bundle.getBundleId(),
        PromotionType.BUNDLE.name(),
        bundle.getName(),
        buildBundleSummary(bundle, services),
        bundle.getOriginalPrice(),
        bundle.getMonthlyPrice(),
        bundle.getBillingCycle(),
        bundle.getSourceUrl(),
        bundle.getLogoUrl(),
        startsAt,
        endsAt,
        services);
  }

  private static String buildBundleSummary(
      Bundle bundle,
      List<PromotionDetailServiceResponse> services) {
    String coveredServices = services.stream()
        .map(PromotionDetailServiceResponse::name)
        .collect(Collectors.joining(", "));

    Integer monthlySavingAmount = calculateMonthlySavingAmount(bundle);
    if (monthlySavingAmount != null && monthlySavingAmount > 0) {
      return coveredServices + " 조합을 번들로 이용하면 월 "
          + formatWon(monthlySavingAmount)
          + " 절약할 수 있어요.";
    }
    return coveredServices + " 조합을 한 번에 관리할 수 있는 번들이에요.";
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
