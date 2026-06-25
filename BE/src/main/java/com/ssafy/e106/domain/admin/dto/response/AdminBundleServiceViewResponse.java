package com.ssafy.e106.domain.admin.dto.response;

public record AdminBundleServiceViewResponse(
    Long serviceId,
    String serviceCode,
    String serviceName
) {
}
