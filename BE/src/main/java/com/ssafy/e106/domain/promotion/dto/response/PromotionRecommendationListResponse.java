package com.ssafy.e106.domain.promotion.dto.response;

import java.util.List;

public record PromotionRecommendationListResponse(
    List<PromotionRecommendationItemResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {
}
