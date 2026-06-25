package com.ssafy.e106.domain.subscription.dto.response;

import java.time.LocalDate;
import java.util.List;

import com.ssafy.e106.domain.subscription.entity.Subscription;

public record SubscriptionListItemResponse(
    Long subscriptionId,
    Long serviceId,
    Long servicePlanId,
    String subscriptionType,
    String bundleCode,
    String serviceName,
    String planName,
    String logoUrl,
    Integer monthlyPrice,
    Integer originalPrice,
    LocalDate nextBillingDate,
    List<SubscriptionCoveredServiceResponse> coveredServices,
    Integer expectedSavingAmount,
    String savingLabel) {

  public static SubscriptionListItemResponse of(
      Subscription subscription,
      Long representativeServiceId,
      List<SubscriptionCoveredServiceResponse> coveredServices) {
    return new SubscriptionListItemResponse(
        subscription.getSubscriptionId(),
        representativeServiceId,
        subscription.getServicePlan() == null ? null : subscription.getServicePlan().getServicePlanId(),
        subscription.getSubscriptionType().name(),
        subscription.getBundle() == null ? null : subscription.getBundle().getCode(),
        subscription.getBundle() == null ? subscription.getService().getName() : subscription.getBundle().getName(),
        subscription.getBundle() == null ? subscription.getServicePlan().getPlanName() : subscription.getBundle().getPlanName(),
        subscription.getBundle() == null ? subscription.getService().getLogoUrl() : subscription.getBundle().getLogoUrl(),
        subscription.getBundle() == null ? subscription.getServicePlan().getMonthlyPrice() : subscription.getBundle().getMonthlyPrice(),
        subscription.getBundle() == null ? null : subscription.getBundle().getOriginalPrice(),
        subscription.getNextBillingDate(),
        coveredServices,
        null,
        null);
  }
}
