package com.ssafy.e106.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.api.NotificationApi
import com.ssafy.e106.data.dto.notification.FcmTokenResponse
import com.ssafy.e106.data.dto.notification.NotificationItem
import com.ssafy.e106.data.dto.notification.NotificationReadResponse
import com.ssafy.e106.data.dto.notification.UpsertFcmTokenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class NotificationRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val notificationApi: NotificationApi,
    private val tokenRepository: TokenRepository,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val _hasUnreadNotifications = MutableStateFlow(false)
    val hasUnreadNotifications: StateFlow<Boolean> = _hasUnreadNotifications.asStateFlow()

    fun syncFcmTokenOnAppStart() {
        syncCurrentFcmToken(trigger = TRIGGER_APP_START, force = false)
    }

    fun syncFcmTokenOnLoginSuccess() {
        val pendingToken = getPendingToken()
        if (pendingToken.isNullOrBlank()) {
            syncCurrentFcmToken(trigger = TRIGGER_LOGIN_SUCCESS, force = true)
        } else {
            enqueueFcmTokenSync(
                token = pendingToken,
                trigger = TRIGGER_LOGIN_SUCCESS,
                force = true,
            )
        }
    }

    fun syncFcmTokenOnNewToken(token: String) {
        enqueueFcmTokenSync(
            token = token,
            trigger = TRIGGER_NEW_TOKEN,
            force = true,
        )
    }

    suspend fun upsertFcmToken(fcmToken: String): Result<FcmTokenResponse> {
        return try {
            val response = notificationApi.upsertFcmToken(
                UpsertFcmTokenRequest(fcmToken = fcmToken),
            )
            if (response.success && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.message ?: "FCM token upsert failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "FCM token upsert exception")
        }
    }

    suspend fun getNotifications(
        unreadOnly: Boolean = false,
        size: Int = 20,
    ): Result<List<NotificationItem>> {
        return try {
            val response = notificationApi.getNotifications(
                unreadOnly = unreadOnly,
                size = size,
            )
            if (response.success && response.data != null) {
                Result.Success(response.data.notifications)
            } else {
                Result.Error(response.message ?: "Notification fetch failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Notification fetch exception")
        }
    }

    suspend fun markAsRead(notificationId: Long): Result<NotificationReadResponse> {
        return try {
            val response = notificationApi.markAsRead(notificationId = notificationId)
            if (response.success && response.data != null) {
                Result.Success(response.data)
            } else {
                Result.Error(response.message ?: "Notification read update failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Notification read update exception")
        }
    }

    suspend fun deleteNotification(notificationId: Long): Result<Unit> {
        return try {
            val response = notificationApi.deleteNotification(notificationId = notificationId)
            if (response.success) {
                Result.Success(Unit)
            } else {
                Result.Error(response.message ?: "Notification delete failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Notification delete exception")
        }
    }

    suspend fun deleteAllNotifications(): Result<Unit> {
        return try {
            val response = notificationApi.deleteAllNotifications()
            if (response.success) {
                Result.Success(Unit)
            } else {
                Result.Error(response.message ?: "Notification clear failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Notification clear exception")
        }
    }

    suspend fun refreshUnreadIndicator(): Result<Boolean> {
        if (!hasAuthenticatedSessionCandidate()) {
            _hasUnreadNotifications.value = false
            return Result.Success(false)
        }

        return try {
            val response = notificationApi.getNotifications(unreadOnly = true, size = 1)
            if (response.success && response.data != null) {
                val hasUnread = response.data.notifications.isNotEmpty()
                _hasUnreadNotifications.value = hasUnread
                Result.Success(hasUnread)
            } else {
                Result.Error(response.message ?: "Unread notification state fetch failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Unread notification state fetch exception")
        }
    }

    fun markUnreadHintReceived() {
        _hasUnreadNotifications.value = true
    }

    fun updateUnreadIndicatorLocally(hasUnread: Boolean) {
        _hasUnreadNotifications.value = hasUnread
    }

    private fun syncCurrentFcmToken(
        trigger: String,
        force: Boolean,
    ) {
        val pendingToken = getPendingToken()
        if (!pendingToken.isNullOrBlank() && shouldAttemptPendingToken(pendingToken, force)) {
            enqueueFcmTokenSync(
                token = pendingToken,
                trigger = trigger,
                force = force,
            )
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                enqueueFcmTokenSync(
                    token = token,
                    trigger = trigger,
                    force = force,
                )
            }
            .addOnFailureListener { throwable ->
                Log.w(TAG, "Failed to fetch FCM token for $trigger", throwable)
            }
    }

    private fun enqueueFcmTokenSync(
        token: String,
        trigger: String,
        force: Boolean,
    ) {
        val normalizedToken = token.trim()
        if (normalizedToken.isBlank()) return

        scope.launch {
            syncFcmTokenInternal(
                token = normalizedToken,
                trigger = trigger,
                force = force,
            )
        }
    }

    private suspend fun syncFcmTokenInternal(
        token: String,
        trigger: String,
        force: Boolean,
    ) {
        syncMutex.withLock {
            if (!hasAuthenticatedSessionCandidate()) {
                cachePendingTokenForAuthenticatedSession(token)
                return
            }

            val pendingToken = getPendingToken()
            val lastSyncedToken = getLastSyncedToken()
            val nowMillis = System.currentTimeMillis()

            if (!force && pendingToken == token && getNextRetryAtMillis() > nowMillis) {
                return
            }

            if (!force && pendingToken.isNullOrBlank() && lastSyncedToken == token) {
                return
            }

            when (val result = upsertFcmToken(token)) {
                is Result.Success -> markTokenSyncSuccess(token)
                is Result.Error -> {
                    Log.w(TAG, "Failed to upsert FCM token for $trigger: ${result.message}")
                    markTokenSyncFailure(token)
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun hasAuthenticatedSessionCandidate(): Boolean {
        return !tokenRepository.getAccessTokenSync().isNullOrBlank() ||
            !tokenRepository.getRefreshTokenSync().isNullOrBlank()
    }

    private fun shouldAttemptPendingToken(
        token: String,
        force: Boolean,
    ): Boolean {
        if (force) return true
        if (token != getLastSyncedToken()) return true
        return getNextRetryAtMillis() <= System.currentTimeMillis()
    }

    private fun cachePendingTokenForAuthenticatedSession(token: String) {
        prefs.edit()
            .putString(KEY_PENDING_TOKEN, token)
            .putInt(KEY_SYNC_FAILURE_COUNT, 0)
            .putLong(KEY_NEXT_RETRY_AT_MILLIS, 0L)
            .apply()
    }

    private fun markTokenSyncSuccess(token: String) {
        prefs.edit()
            .putString(KEY_LAST_SYNCED_TOKEN, token)
            .remove(KEY_PENDING_TOKEN)
            .putInt(KEY_SYNC_FAILURE_COUNT, 0)
            .putLong(KEY_NEXT_RETRY_AT_MILLIS, 0L)
            .apply()
    }

    private fun markTokenSyncFailure(token: String) {
        val nextFailureCount = if (getPendingToken() == token) {
            getSyncFailureCount() + 1
        } else {
            1
        }
        val retryDelayMillis = RETRY_BACKOFF_MILLIS[
            (nextFailureCount - 1).coerceIn(0, RETRY_BACKOFF_MILLIS.lastIndex)
        ]

        prefs.edit()
            .putString(KEY_PENDING_TOKEN, token)
            .putInt(KEY_SYNC_FAILURE_COUNT, nextFailureCount)
            .putLong(KEY_NEXT_RETRY_AT_MILLIS, System.currentTimeMillis() + retryDelayMillis)
            .apply()
    }

    private fun getLastSyncedToken(): String? = prefs.getString(KEY_LAST_SYNCED_TOKEN, null)

    private fun getPendingToken(): String? = prefs.getString(KEY_PENDING_TOKEN, null)

    private fun getSyncFailureCount(): Int = prefs.getInt(KEY_SYNC_FAILURE_COUNT, 0)

    private fun getNextRetryAtMillis(): Long = prefs.getLong(KEY_NEXT_RETRY_AT_MILLIS, 0L)

    private companion object {
        const val TAG = "NotificationRepository"
        const val PREFS_NAME = "notification_token_sync_prefs"
        const val KEY_LAST_SYNCED_TOKEN = "last_synced_token"
        const val KEY_PENDING_TOKEN = "pending_token"
        const val KEY_SYNC_FAILURE_COUNT = "sync_failure_count"
        const val KEY_NEXT_RETRY_AT_MILLIS = "next_retry_at_millis"

        const val TRIGGER_APP_START = "app_start"
        const val TRIGGER_LOGIN_SUCCESS = "login_success"
        const val TRIGGER_NEW_TOKEN = "new_token"

        val RETRY_BACKOFF_MILLIS = longArrayOf(
            5 * 60 * 1000L,
            30 * 60 * 1000L,
            2 * 60 * 60 * 1000L,
        )
    }
}
