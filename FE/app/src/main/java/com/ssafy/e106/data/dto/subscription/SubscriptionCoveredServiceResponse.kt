package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionCoveredServiceResponse(
    val serviceId: Long,
    val serviceCode: String? = null,
    val serviceName: String,
    val logoUrl: String? = null,
)
