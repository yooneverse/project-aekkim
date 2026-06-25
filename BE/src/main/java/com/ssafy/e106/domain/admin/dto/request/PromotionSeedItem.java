package com.ssafy.e106.domain.admin.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.e106.domain.promotion.enums.PromotionType;

public record PromotionSeedItem(
    PromotionType promotionType,
    String title,
    String summary,
    Integer originalPrice,
    Integer discountPrice,
    String billingCycle,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    String sourceUrl,
    String imageUrl,
    List<PromotionSeedServiceItem> services) {
}
