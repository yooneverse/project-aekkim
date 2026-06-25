package com.ssafy.e106.domain.subscription.dto.response;

import com.ssafy.e106.domain.subscription.entity.ServicePlan;

public record ServicePlanResponse(
    Long servicePlanId,
    String planName,
    String billingCycle,
    Integer monthlyPrice) {

  public static ServicePlanResponse from(ServicePlan servicePlan) {
    return new ServicePlanResponse(
        servicePlan.getServicePlanId(),
        servicePlan.getPlanName(),
        servicePlan.getBillingCycle(),
        servicePlan.getMonthlyPrice());
  }
}
