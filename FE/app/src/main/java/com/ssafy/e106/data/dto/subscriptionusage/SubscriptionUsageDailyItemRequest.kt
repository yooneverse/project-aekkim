package com.ssafy.e106.data.dto.subscriptionusage

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionUsageDailyItemRequest(
    val serviceId: Long,
    val usageDate: String,
    val usedMinutes: Int,
)
