package com.ssafy.e106.domain.admin.dto.request;

import java.time.LocalDateTime;
import java.util.List;

public record BundleSeedPayload(
    String jobType,
    LocalDateTime mergedAt,
    Integer count,
    List<BundleSeedItem> bundles
) {
}
