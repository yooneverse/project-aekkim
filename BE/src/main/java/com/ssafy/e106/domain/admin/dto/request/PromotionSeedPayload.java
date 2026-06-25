package com.ssafy.e106.domain.admin.dto.request;

import java.time.LocalDateTime;
import java.util.List;

public record PromotionSeedPayload(
    String jobType,
    LocalDateTime mergedAt,
    Integer count,
    List<PromotionSeedItem> promotions) {
}
