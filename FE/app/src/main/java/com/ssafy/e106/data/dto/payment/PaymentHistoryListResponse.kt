package com.ssafy.e106.data.dto.payment

import kotlinx.serialization.Serializable

@Serializable
data class PaymentHistoryListResponse(
    val count: Int,
    val payments: List<PaymentHistoryResponse>,
)
