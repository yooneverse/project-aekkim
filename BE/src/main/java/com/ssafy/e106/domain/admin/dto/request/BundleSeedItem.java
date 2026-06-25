package com.ssafy.e106.domain.admin.dto.request;

import java.util.List;

public record BundleSeedItem(
    String code,
    String name,
    String planName,
    String billingCycle,
    Integer monthlyPrice,
    Integer originalPrice,
    String logoUrl,
    String sourceUrl,
    List<BundleSeedServiceItem> services
) {
}
