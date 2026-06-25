package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class BundleListItemResponse(
    val bundleId: Long,
    val code: String,
    val name: String,
    val planName: String,
    val billingCycle: String,
    val monthlyPrice: Int,
    val originalPrice: Int? = null,
    val logoUrl: String? = null,
    val coveredServices: List<SubscriptionCoveredServiceResponse> = emptyList(),
)
