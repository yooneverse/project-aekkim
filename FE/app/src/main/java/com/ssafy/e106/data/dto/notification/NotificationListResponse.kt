package com.ssafy.e106.data.dto.notification

import kotlinx.serialization.Serializable

@Serializable
data class NotificationListResponse(
    val notifications: List<NotificationItem>,
)
