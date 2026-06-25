package com.ssafy.e106.domain.admin.dto.request;

import com.ssafy.e106.domain.subscription.enums.ServiceCategory;

public record ServiceSeedItem(
    String code,
    String name,
    ServiceCategory category,
    String logoUrl,
    String cancelGuideUrl,
    String customerServicePhone,
    String contactEmail) {
}
