package com.ssafy.e106.domain.promotion.service;

import java.util.List;

public record PromotionRecommendationEvaluation(
    int score,
    List<String> reasonLabels) {
}
