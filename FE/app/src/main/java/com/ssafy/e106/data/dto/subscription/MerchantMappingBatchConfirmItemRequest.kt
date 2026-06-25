package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class MerchantMappingBatchConfirmItemRequest(
    val merchantRaw: String,
    val serviceId: Long,
    val servicePlanId: Long,
    val nextBillingDate: String? = null,
)
