package com.ssafy.e106.feature.notification

import com.ssafy.e106.data.dto.notification.NotificationType

data class NotificationUiState(
    val notifications: List<NotificationListItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isEmpty: Boolean = false,
    val error: String? = null,
    val readingNotificationIds: Set<Long> = emptySet(),
    val deletingNotificationIds: Set<Long> = emptySet(),
    val isClearingAll: Boolean = false,
)

data class NotificationListItem(
    val notificationId: Long,
    val type: NotificationType,
    val referenceId: Long? = null,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val readAt: String? = null,
    val sentAt: String? = null,
)

sealed interface NotificationUiEffect {
    data class ShowToast(val message: String) : NotificationUiEffect
    data class NavigateToCheckIn(val subscriptionId: Long) : NotificationUiEffect
    data class NavigateToSubscriptionDetail(val subscriptionId: Long) : NotificationUiEffect
    data class NavigateToPromotionDetail(val promotionId: Long) : NotificationUiEffect
}

sealed interface NotificationIntent {
    data object LoadNotifications : NotificationIntent
    data object RefreshNotifications : NotificationIntent
    data object RetryLoad : NotificationIntent
    data class OpenNotification(val notificationId: Long) : NotificationIntent
    data class DeleteNotification(val notificationId: Long) : NotificationIntent
    data object DeleteAllNotifications : NotificationIntent
}
