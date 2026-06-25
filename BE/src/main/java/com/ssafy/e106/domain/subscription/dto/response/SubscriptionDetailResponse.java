package com.ssafy.e106.domain.subscription.dto.response;

import java.time.LocalDate;
import java.util.List;

import com.ssafy.e106.domain.subscription.entity.Subscription;

public record SubscriptionDetailResponse(
    Long subscriptionId,
    String subscriptionType,
    String bundleCode,
    String serviceName,
    String planName,
    String logoUrl,
    Integer monthlyPrice,
    Integer originalPrice,
    String billingCycle,
    LocalDate nextBillingDate,
    Integer daysUntilBilling,
    List<SubscriptionCoveredServiceResponse> coveredServices,
    List<SubscriptionRecentUsageResponse> recentUsage,
    SubscriptionRecommendationResponse recommendation,
    String cancelGuideUrl,
    String customerServicePhone,
    String contactEmail) {

  public static SubscriptionDetailResponse of(
      Subscription subscription,
      String serviceName,
      String planName,
      String logoUrl,
      Integer monthlyPrice,
      Integer originalPrice,
      String billingCycle,
      Integer daysUntilBilling,
      List<SubscriptionCoveredServiceResponse> coveredServices,
      List<SubscriptionRecentUsageResponse> recentUsage,
      SubscriptionRecommendationResponse recommendation) {
    return new SubscriptionDetailResponse(
        subscription.getSubscriptionId(),
        subscription.getSubscriptionType().name(),
        subscription.getBundle() == null ? null : subscription.getBundle().getCode(),
        serviceName,
        planName,
        logoUrl,
        monthlyPrice,
        originalPrice,
        billingCycle,
        subscription.getNextBillingDate(),
        daysUntilBilling,
        coveredServices,
        recentUsage,
        recommendation,
        subscription.getService() == null ? null : subscription.getService().getCancelGuideUrl(),
        subscription.getService() == null ? null : subscription.getService().getCustomerServicePhone(),
        subscription.getService() == null ? null : subscription.getService().getContactEmail());
  }
}
