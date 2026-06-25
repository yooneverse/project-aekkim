package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionListResponse(
    val monthlyTotalAmount: Int,
    val subscriptions: List<SubscriptionListItemResponse>,
)
