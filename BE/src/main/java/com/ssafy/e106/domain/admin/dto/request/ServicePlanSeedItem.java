package com.ssafy.e106.domain.admin.dto.request;

public record ServicePlanSeedItem(
    String serviceCode,
    String planName,
    String billingCycle,
    Integer monthlyPrice) {
}
