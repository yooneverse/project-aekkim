package com.ssafy.e106.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.dto.notification.NotificationItem
import com.ssafy.e106.data.dto.notification.NotificationType
import com.ssafy.e106.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<NotificationUiEffect>(replay = 0)
    val uiEffect: SharedFlow<NotificationUiEffect> = _uiEffect.asSharedFlow()

    private var hasLoadedNotifications = false

    fun onIntent(intent: NotificationIntent) {
        when (intent) {
            NotificationIntent.LoadNotifications -> loadNotifications(forceRefresh = false)
            NotificationIntent.RefreshNotifications -> loadNotifications(forceRefresh = true)
            NotificationIntent.RetryLoad -> loadNotifications(forceRefresh = true)
            is NotificationIntent.OpenNotification -> openNotification(intent.notificationId)
            is NotificationIntent.DeleteNotification -> deleteNotification(intent.notificationId)
            NotificationIntent.DeleteAllNotifications -> deleteAllNotifications()
        }
    }

    private fun loadNotifications(forceRefresh: Boolean) {
        if (hasLoadedNotifications && !forceRefresh) return

        val hasExistingData = _uiState.value.notifications.isNotEmpty()
        _uiState.update { state ->
            state.copy(
                isLoading = !hasExistingData,
                isRefreshing = hasExistingData && forceRefresh,
                error = if (hasExistingData) null else state.error,
            )
        }

        viewModelScope.launch {
            when (val result = notificationRepository.getNotifications()) {
                is Result.Success -> {
                    hasLoadedNotifications = true
                    _uiState.update { state ->
                        state.copy(
                            notifications = result.data.map { item -> item.toUiItem() },
                            isLoading = false,
                            isRefreshing = false,
                            isEmpty = result.data.isEmpty(),
                            error = null,
                        )
                    }
                    refreshUnreadIndicatorSilently()
                }

                is Result.Error -> {
                    if (_uiState.value.notifications.isNotEmpty()) {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                isRefreshing = false,
                            )
                        }
                        _uiEffect.emit(
                            NotificationUiEffect.ShowToast(
                                result.message.ifBlank { "Failed to refresh notifications." },
                            ),
                        )
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                isRefreshing = false,
                                isEmpty = false,
                                error = result.message,
                            )
                        }
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun openNotification(notificationId: Long) {
        val notification = _uiState.value.notifications.firstOrNull {
            it.notificationId == notificationId
        } ?: return

        if (_uiState.value.isClearingAll || _uiState.value.deletingNotificationIds.isNotEmpty()) return
        if (notificationId in _uiState.value.readingNotificationIds) return

        val destination = notification.resolveDestination()
        if (notification.isRead) {
            emitDestinationOrToast(destination)
            return
        }

        _uiState.update { state ->
            state.copy(
                readingNotificationIds = state.readingNotificationIds + notificationId,
            )
        }

        viewModelScope.launch {
            when (val result = notificationRepository.markAsRead(notificationId)) {
                is Result.Success -> {
                    var hasUnreadNotifications = false
                    _uiState.update { state ->
                        val updatedNotifications = state.notifications.map { item ->
                            if (item.notificationId == notificationId) {
                                item.copy(
                                    isRead = result.data.isRead,
                                    readAt = result.data.readAt,
                                )
                            } else {
                                item
                            }
                        }
                        hasUnreadNotifications = updatedNotifications.any { !it.isRead }
                        state.copy(
                            notifications = updatedNotifications,
                            readingNotificationIds = state.readingNotificationIds - notificationId,
                        )
                    }
                    notificationRepository.updateUnreadIndicatorLocally(hasUnreadNotifications)
                    refreshUnreadIndicatorSilently()
                    emitDestinationOrToast(destination)
                }

                is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            readingNotificationIds = state.readingNotificationIds - notificationId,
                        )
                    }
                    _uiEffect.emit(
                        NotificationUiEffect.ShowToast(
                            result.message.ifBlank { "Failed to mark notification as read." },
                        ),
                    )
                    destination?.let { emitDestination(it) }
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun deleteNotification(notificationId: Long) {
        val state = _uiState.value
        if (state.isClearingAll || state.deletingNotificationIds.isNotEmpty()) return

        val previousNotifications = state.notifications
        val updatedNotifications = previousNotifications.filterNot { it.notificationId == notificationId }
        if (previousNotifications.size == updatedNotifications.size) return

        _uiState.update { current ->
            current.copy(
                notifications = updatedNotifications,
                isEmpty = updatedNotifications.isEmpty(),
                deletingNotificationIds = current.deletingNotificationIds + notificationId,
                error = null,
            )
        }
        notificationRepository.updateUnreadIndicatorLocally(updatedNotifications.any { !it.isRead })

        viewModelScope.launch {
            when (val result = notificationRepository.deleteNotification(notificationId)) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            deletingNotificationIds = current.deletingNotificationIds - notificationId,
                            isEmpty = current.notifications.isEmpty(),
                        )
                    }
                    refreshUnreadIndicatorSilently()
                }

                is Result.Error -> {
                    _uiState.update { current ->
                        current.copy(
                            notifications = previousNotifications,
                            isEmpty = previousNotifications.isEmpty(),
                            deletingNotificationIds = current.deletingNotificationIds - notificationId,
                        )
                    }
                    notificationRepository.updateUnreadIndicatorLocally(previousNotifications.any { !it.isRead })
                    _uiEffect.emit(
                        NotificationUiEffect.ShowToast(
                            result.message.ifBlank { "Failed to delete notification." },
                        ),
                    )
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun deleteAllNotifications() {
        val state = _uiState.value
        val previousNotifications = state.notifications
        if (previousNotifications.isEmpty()) return
        if (state.isClearingAll || state.deletingNotificationIds.isNotEmpty()) return

        _uiState.update { current ->
            current.copy(
                notifications = emptyList(),
                isEmpty = true,
                isClearingAll = true,
                error = null,
            )
        }
        notificationRepository.updateUnreadIndicatorLocally(false)

        viewModelScope.launch {
            when (val result = notificationRepository.deleteAllNotifications()) {
                is Result.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            isClearingAll = false,
                            isEmpty = true,
                        )
                    }
                    refreshUnreadIndicatorSilently()
                }

                is Result.Error -> {
                    _uiState.update { current ->
                        current.copy(
                            notifications = previousNotifications,
                            isEmpty = previousNotifications.isEmpty(),
                            isClearingAll = false,
                        )
                    }
                    notificationRepository.updateUnreadIndicatorLocally(previousNotifications.any { !it.isRead })
                    _uiEffect.emit(
                        NotificationUiEffect.ShowToast(
                            result.message.ifBlank { "Failed to clear notifications." },
                        ),
                    )
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun emitDestinationOrToast(destination: NotificationDestination?) {
        viewModelScope.launch {
            if (destination == null) {
                _uiEffect.emit(
                    NotificationUiEffect.ShowToast(
                        "This notification has no linked destination.",
                    ),
                )
                return@launch
            }
            emitDestination(destination)
        }
    }

    private suspend fun emitDestination(destination: NotificationDestination) {
        when (destination) {
            is NotificationDestination.CheckIn -> {
                _uiEffect.emit(
                    NotificationUiEffect.NavigateToCheckIn(destination.subscriptionId),
                )
            }

            is NotificationDestination.PromotionDetail -> {
                _uiEffect.emit(
                    NotificationUiEffect.NavigateToPromotionDetail(destination.promotionId),
                )
            }

            is NotificationDestination.SubscriptionDetail -> {
                _uiEffect.emit(
                    NotificationUiEffect.NavigateToSubscriptionDetail(destination.subscriptionId),
                )
            }
        }
    }

    private fun refreshUnreadIndicatorSilently() {
        viewModelScope.launch {
            notificationRepository.refreshUnreadIndicator()
        }
    }

    private fun NotificationItem.toUiItem(): NotificationListItem {
        return NotificationListItem(
            notificationId = notificationId,
            type = type,
            referenceId = referenceId,
            title = title,
            body = body,
            isRead = isRead,
            readAt = readAt,
            sentAt = sentAt,
        )
    }

    private fun NotificationListItem.resolveDestination(): NotificationDestination? {
        val targetId = referenceId ?: return null
        return when (type) {
            NotificationType.CHECKIN ->
                NotificationDestination.CheckIn(subscriptionId = targetId)

            NotificationType.CHURN_REVIEW ->
                NotificationDestination.SubscriptionDetail(subscriptionId = targetId)

            NotificationType.PROMO ->
                NotificationDestination.PromotionDetail(promotionId = targetId)
        }
    }
}

private sealed interface NotificationDestination {
    data class CheckIn(val subscriptionId: Long) : NotificationDestination
    data class SubscriptionDetail(val subscriptionId: Long) : NotificationDestination
    data class PromotionDetail(val promotionId: Long) : NotificationDestination
}
