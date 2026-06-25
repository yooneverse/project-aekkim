package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class MerchantMappingBatchConfirmItemResponse(
    val subscriptionId: Long,
    val serviceId: Long,
    val serviceName: String,
    val servicePlanId: Long,
    val monthlyPrice: Int,
    val nextBillingDate: String? = null,
)
