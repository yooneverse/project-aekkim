package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class CheckInCreateResponse(
    val checkinRecordId: Long,
    val serviceId: Long,
    val cycleYm: String,
    val response: String,
    val respondedAt: String,
)
