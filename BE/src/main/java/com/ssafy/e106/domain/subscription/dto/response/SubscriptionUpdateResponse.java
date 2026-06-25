package com.ssafy.e106.domain.subscription.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.ssafy.e106.domain.subscription.entity.Subscription;

public record SubscriptionUpdateResponse(
    Long subscriptionId,
    Long userId,
    String subscriptionType,
    Long serviceId,
    Long servicePlanId,
    String bundleCode,
    Integer monthlyPrice,
    Integer originalPrice,
    LocalDate nextBillingDate,
    LocalDateTime updatedAt) {

  public static SubscriptionUpdateResponse of(Subscription subscription) {
    return new SubscriptionUpdateResponse(
        subscription.getSubscriptionId(),
        subscription.getUserId(),
        subscription.getSubscriptionType().name(),
        subscription.getService() == null ? null : subscription.getService().getServiceId(),
        subscription.getServicePlan() == null ? null : subscription.getServicePlan().getServicePlanId(),
        subscription.getBundle() == null ? null : subscription.getBundle().getCode(),
        subscription.getBundle() == null
            ? subscription.getServicePlan().getMonthlyPrice()
            : subscription.getBundle().getMonthlyPrice(),
        subscription.getBundle() == null ? null : subscription.getBundle().getOriginalPrice(),
        subscription.getNextBillingDate(),
        subscription.getUpdatedAt());
  }
}
