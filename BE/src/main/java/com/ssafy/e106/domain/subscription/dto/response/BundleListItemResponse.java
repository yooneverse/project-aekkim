package com.ssafy.e106.domain.subscription.dto.response;

import java.util.List;

import com.ssafy.e106.domain.subscription.entity.Bundle;

public record BundleListItemResponse(
    Long bundleId,
    String code,
    String name,
    String planName,
    String billingCycle,
    Integer monthlyPrice,
    Integer originalPrice,
    String logoUrl,
    List<SubscriptionCoveredServiceResponse> coveredServices) {

  public static BundleListItemResponse of(
      Bundle bundle,
      List<SubscriptionCoveredServiceResponse> coveredServices) {
    return new BundleListItemResponse(
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
