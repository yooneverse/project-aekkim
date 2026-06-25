package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionUpdateRequest(
    val servicePlanId: Long? = null,
    val bundleCode: String? = null,
    val nextBillingDate: String? = null,
)
