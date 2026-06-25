package com.ssafy.e106.domain.admin.dto.response;

import java.util.List;

public record AdminBundleViewResponse(
    Long bundleId,
    String code,
    String name,
    String planName,
    String billingCycle,
    Integer monthlyPrice,
    Integer originalPrice,
    String logoUrl,
    String sourceUrl,
    List<AdminBundleServiceViewResponse> services
) {
}
