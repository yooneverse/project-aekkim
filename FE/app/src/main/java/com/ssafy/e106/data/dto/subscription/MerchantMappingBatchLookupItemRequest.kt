package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class MerchantMappingBatchLookupItemRequest(
    val merchantRaw: String,
    val predictedServiceId: Long,
    val predictedServicePlanId: Long,
)
