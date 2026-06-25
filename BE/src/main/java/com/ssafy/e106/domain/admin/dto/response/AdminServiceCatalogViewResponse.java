package com.ssafy.e106.domain.admin.dto.response;

import java.util.List;

public record AdminServiceCatalogViewResponse(
    Long serviceId,
    String code,
    String name,
    String category,
    String logoUrl,
    List<AdminServicePlanViewResponse> plans) {
}
