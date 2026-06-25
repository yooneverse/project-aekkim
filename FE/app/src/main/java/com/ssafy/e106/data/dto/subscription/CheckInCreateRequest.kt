package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class CheckInCreateRequest(
    val serviceId: Long,
    val cycleYm: String,
    val response: String,
)
