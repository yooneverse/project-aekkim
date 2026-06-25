package com.ssafy.e106.data.dto.subscriptionusage

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionUsageDailyPointResponse(
    val usageDate: String,
    val totalUsedMinutes: Int,
    val ottUsedMinutes: Int,
    val musicUsedMinutes: Int,
    val aiUsedMinutes: Int,
    val dominantCategory: String? = null,
)
