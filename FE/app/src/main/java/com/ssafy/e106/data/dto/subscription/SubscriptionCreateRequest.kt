package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionCreateRequest(
    val subscriptionType: String,
    val serviceId: Long? = null,
    val servicePlanId: Long? = null,
    val bundleCode: String? = null,
    val nextBillingDate: String? = null,
)
