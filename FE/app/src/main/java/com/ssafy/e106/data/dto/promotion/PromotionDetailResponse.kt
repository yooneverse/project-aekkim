package com.ssafy.e106.data.dto.promotion

import kotlinx.serialization.Serializable

@Serializable
data class PromotionDetailResponse(
    val promotionId: Long,
    val promotionType: String,
    val title: String,
    val summary: String? = null,
    val originalPrice: Int? = null,
    val discountPrice: Int? = null,
    val sourceUrl: String? = null,
    val imageUrl: String? = null,
    val startsAt: String,
    val endsAt: String,
    val services: List<PromotionDetailServiceResponse> = emptyList(),
)

@Serializable
data class PromotionDetailServiceResponse(
    val serviceId: Long,
    val code: String,
    val name: String,
    val logoUrl: String? = null,
)
