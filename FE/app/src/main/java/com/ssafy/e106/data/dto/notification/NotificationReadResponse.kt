package com.ssafy.e106.data.dto.notification

import kotlinx.serialization.Serializable

@Serializable
data class NotificationReadResponse(
    val notificationId: Long,
    val isRead: Boolean,
    val readAt: String? = null,
)
