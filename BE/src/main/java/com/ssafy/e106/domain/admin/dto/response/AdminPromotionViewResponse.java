package com.ssafy.e106.domain.admin.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AdminPromotionViewResponse(
    Long promotionId,
    String promotionType,
    String title,
    Integer originalPrice,
    Integer discountPrice,
    String billingCycle,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    String imageUrl,
    List<AdminPromotionServiceViewResponse> services) {
}
