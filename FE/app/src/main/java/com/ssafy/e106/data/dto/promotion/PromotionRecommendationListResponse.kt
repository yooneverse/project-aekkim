package com.ssafy.e106.data.dto.promotion

import kotlinx.serialization.Serializable

@Serializable
data class PromotionRecommendationCategoryListResponse(
    val categories: List<PromotionRecommendationCategoryResponse> = emptyList(),
    val size: Int = 0,
)

@Serializable
data class PromotionRecommendationCategoryResponse(
    val category: String,
    val bundles: List<PromotionRecommendationItemResponse> = emptyList(),
    val promotions: List<PromotionRecommendationItemResponse> = emptyList(),
    val cardBenefits: List<PromotionRecommendationItemResponse> = emptyList(),
)

@Serializable
data class PromotionRecommendationItemResponse(
    val promotionId: Long,
    val promotionType: String,
    val title: String,
    val summary: String? = null,
    val originalPrice: Int? = null,
    val discountPrice: Int? = null,
    val billingCycle: String? = null,
    val sourceUrl: String? = null,
    val imageUrl: String? = null,
    val recommendationScore: Int = 0,
    val recommendationReasons: List<String> = emptyList(),
    val startsAt: String,
    val endsAt: String,
    val services: List<PromotionRecommendationServiceResponse> = emptyList(),
)

@Serializable
data class PromotionRecommendationServiceResponse(
    val serviceId: Long,
    val serviceName: String,
    val logoUrl: String? = null,
)
