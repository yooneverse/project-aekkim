package com.ssafy.e106.data.dto.notification

import kotlinx.serialization.Serializable

@Serializable
data class FcmTokenResponse(
    val fcmTokenId: Long,
    val fcmToken: String,
    val updatedAt: String,
)
