package com.ssafy.e106.domain.admin.dto.response;

public record AdminPromotionServiceViewResponse(
    Long serviceId,
    String serviceCode,
    String serviceName) {
}
