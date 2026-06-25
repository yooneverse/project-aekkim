package com.ssafy.e106.domain.subscription.dto.response;

import java.util.List;

import com.ssafy.e106.domain.subscription.entity.Bundle;

public record BundleDetailResponse(
    Long bundleId,
    String code,
    String name,
    String planName,
    String billingCycle,
    Integer monthlyPrice,
    Integer originalPrice,
    String logoUrl,
    List<SubscriptionCoveredServiceResponse> coveredServices) {

  public static BundleDetailResponse of(
      Bundle bundle,
      List<SubscriptionCoveredServiceResponse> coveredServices) {
    return new BundleDetailResponse(
        bundle.getBundleId(),
        bundle.getCode(),
        bundle.getName(),
        bundle.getPlanName(),
        bundle.getBillingCycle(),
        bundle.getMonthlyPrice(),
        bundle.getOriginalPrice(),
        bundle.getLogoUrl(),
        coveredServices);
  }
}
