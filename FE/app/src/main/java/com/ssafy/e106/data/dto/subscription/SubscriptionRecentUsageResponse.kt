package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionRecentUsageResponse(
    val cycleYm: String,
    val response: String,
    val serviceId: Long? = null,
    val serviceName: String? = null,
)
