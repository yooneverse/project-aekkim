package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class MerchantMappingBatchLookupItemResponse(
    val merchantRaw: String,
    val matched: Boolean,
    val serviceId: Long? = null,
    val servicePlanId: Long? = null,
    val hitCount: Int? = null,
)
