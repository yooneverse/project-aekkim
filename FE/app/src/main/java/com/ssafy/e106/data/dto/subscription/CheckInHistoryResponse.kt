package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class CheckInHistoryResponse(
    val checkins: List<CheckInHistoryItemResponse> = emptyList(),
)
