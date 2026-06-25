package com.ssafy.e106.data.dto.subscriptionusage

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionUsageDailyUpsertResponse(
    val savedCount: Int,
)
