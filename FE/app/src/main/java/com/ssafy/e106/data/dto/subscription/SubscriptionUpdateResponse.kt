package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionUpdateResponse(
    val subscriptionId: Long,
    val userId: Long,
    val subscriptionType: String = "SINGLE",
    val serviceId: Long? = null,
    val servicePlanId: Long? = null,
    val bundleCode: String? = null,
    val monthlyPrice: Int,
    val nextBillingDate: String? = null,
    val updatedAt: String? = null,
)
