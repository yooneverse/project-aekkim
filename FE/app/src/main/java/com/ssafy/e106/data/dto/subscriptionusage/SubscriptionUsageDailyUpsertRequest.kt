package com.ssafy.e106.data.dto.subscriptionusage

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionUsageDailyUpsertRequest(
    val items: List<SubscriptionUsageDailyItemRequest>,
)
