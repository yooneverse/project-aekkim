package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class MerchantMappingBatchConfirmResponse(
    val results: List<MerchantMappingBatchConfirmItemResponse> = emptyList(),
)
