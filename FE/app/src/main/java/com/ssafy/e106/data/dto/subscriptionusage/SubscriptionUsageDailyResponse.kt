package com.ssafy.e106.data.dto.subscriptionusage

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionUsageDailyResponse(
    val days: Int,
    val subscriptionId: Long? = null,
    val items: List<SubscriptionUsageDailyPointResponse> = emptyList(),
)
