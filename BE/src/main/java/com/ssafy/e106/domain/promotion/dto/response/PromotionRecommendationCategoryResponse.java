package com.ssafy.e106.domain.promotion.dto.response;

import java.util.List;

public record PromotionRecommendationCategoryResponse(
    String category,
    List<PromotionRecommendationItemResponse> bundles,
    List<PromotionRecommendationItemResponse> promotions,
    List<PromotionRecommendationItemResponse> cardBenefits) {
}
