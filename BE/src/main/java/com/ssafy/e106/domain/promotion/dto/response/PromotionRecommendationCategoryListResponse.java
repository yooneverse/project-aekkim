package com.ssafy.e106.domain.promotion.dto.response;

import java.util.List;

public record PromotionRecommendationCategoryListResponse(
    List<PromotionRecommendationCategoryResponse> categories,
    int size) {
}
