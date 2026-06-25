package com.ssafy.e106.data.dto.payment

import kotlinx.serialization.Serializable

@Serializable
data class PaymentHistoryResponse(
    val paymentHistoryId: Long,
    val paymentId: String,
    val merchantRaw: String,
    val amount: Int,
    val paymentDate: String,
    val category: String,
    val createdAt: String,
    val updatedAt: String,
)
