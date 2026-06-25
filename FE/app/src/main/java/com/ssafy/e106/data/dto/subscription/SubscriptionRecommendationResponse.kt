package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionRecommendationResponse(
    val promotionId: Long,
    val promotionType: String,
    val title: String,
    val monthlySavingAmount: Int? = null,
    val billingCycle: String? = null,
    val headline: String,
    val imageUrl: String? = null,
    val services: List<SubscriptionRecommendationServiceResponse> = emptyList(),
)

@Serializable
data class SubscriptionRecommendationServiceResponse(
    val serviceId: Long,
    val serviceName: String,
    val logoUrl: String? = null,
)
