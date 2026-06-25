package com.ssafy.e106.domain.admin.dto.response;

public record AdminServicePlanViewResponse(
    Long servicePlanId,
    String planName,
    String billingCycle,
    Integer monthlyPrice) {
}
