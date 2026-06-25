package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionDetailResponse(
    val subscriptionId: Long,
    val subscriptionType: String = "SINGLE",
    val bundleCode: String? = null,
    val serviceName: String,
    val planName: String,
    val logoUrl: String? = null,
    val monthlyPrice: Int,
    val billingCycle: String? = null,
    val nextBillingDate: String? = null,
    val daysUntilBilling: Int? = null,
    val coveredServices: List<SubscriptionCoveredServiceResponse> = emptyList(),
    val recentUsage: List<SubscriptionRecentUsageResponse> = emptyList(),
    val recommendation: SubscriptionRecommendationResponse? = null,
    val cancelGuideUrl: String? = null,
    val customerServicePhone: String? = null,
    val contactEmail: String? = null,
)
