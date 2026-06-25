package com.ssafy.e106.data.dto.subscriptionusage

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionUsageReportSummaryResponse(
    val windowDays: Int,
    val totalUsedMinutes: Int,
    val activeSubscriptionCount: Int,
    val lowUsageSubscriptionCount: Int,
    val mostUsedSubscriptionName: String? = null,
    val mostUsedSubscriptionMinutes: Int? = null,
)
