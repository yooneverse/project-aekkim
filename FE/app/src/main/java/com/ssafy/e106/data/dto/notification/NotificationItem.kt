package com.ssafy.e106.data.dto.notification

import kotlinx.serialization.Serializable

@Serializable
data class NotificationItem(
    val notificationId: Long,
    val type: NotificationType,
    val referenceId: Long? = null,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val readAt: String? = null,
    val sentAt: String? = null,
)
