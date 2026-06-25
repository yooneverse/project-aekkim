package com.ssafy.e106.domain.subscription.dto.response;

import java.time.LocalDate;

import com.ssafy.e106.domain.subscription.entity.Subscription;

public record MerchantMappingBatchConfirmItemResponse(
    Long subscriptionId,
    Long serviceId,
    String serviceName,
    Long servicePlanId,
    Integer monthlyPrice,
    LocalDate nextBillingDate) {

  public static MerchantMappingBatchConfirmItemResponse from(Subscription subscription) {
    return new MerchantMappingBatchConfirmItemResponse(
        subscription.getSubscriptionId(),
        subscription.getService().getServiceId(),
        subscription.getService().getName(),
        subscription.getServicePlan().getServicePlanId(),
        subscription.getServicePlan().getMonthlyPrice(),
        subscription.getNextBillingDate());
  }
}
