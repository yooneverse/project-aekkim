package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class MerchantMappingBatchConfirmRequest(
    val items: List<MerchantMappingBatchConfirmItemRequest>,
)
