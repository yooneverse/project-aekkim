package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class CheckInHistoryItemResponse(
    val checkinRecordId: Long,
    val serviceId: Long,
    val serviceName: String,
    val cycleYm: String,
    val response: String,
    val respondedAt: String,
)
