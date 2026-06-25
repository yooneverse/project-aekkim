package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class MerchantMappingBatchLookupResponse(
    val results: List<MerchantMappingBatchLookupItemResponse> = emptyList(),
)
