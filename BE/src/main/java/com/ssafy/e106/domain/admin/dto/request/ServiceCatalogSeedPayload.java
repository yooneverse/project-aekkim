package com.ssafy.e106.domain.admin.dto.request;

import java.time.LocalDateTime;
import java.util.List;

public record ServiceCatalogSeedPayload(
    String jobType,
    LocalDateTime mergedAt,
    List<ServiceSeedItem> services,
    List<ServicePlanSeedItem> servicePlans) {
}
