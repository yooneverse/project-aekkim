package com.ssafy.e106.data.manager

import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ssafy.e106.R
import com.ssafy.e106.app.MainActivity
import com.ssafy.e106.data.dto.notification.NotificationType
import com.ssafy.e106.data.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AekkimFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var notificationRepository: NotificationRepository

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        notificationRepository.syncFcmTokenOnNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val payload = parsePayload(remoteMessage) ?: return
        notificationRepository.markUnreadHintReceived()
        showNotification(payload)
    }

    private fun parsePayload(remoteMessage: RemoteMessage): NotificationPayload? {
        val type = remoteMessage.data["type"]
            ?.let { rawType -> runCatching { NotificationType.valueOf(rawType) }.getOrNull() }
            ?: return null
        val referenceId = remoteMessage.data["referenceId"]
            ?.takeIf { value -> value.isNotBlank() }
            ?: return null
        val title = remoteMessage.data["title"]
            .orEmpty()
            .ifBlank { remoteMessage.notification?.title.orEmpty() }
            .ifBlank { type.defaultTitle() }
        val body = remoteMessage.data["body"]
            .orEmpty()
            .ifBlank { remoteMessage.notification?.body.orEmpty() }
            .ifBlank { type.defaultBody() }

        return NotificationPayload(
            type = type,
            referenceId = referenceId,
            title = title,
            body = body,
        )
    }

    private fun showNotification(payload: NotificationPayload) {
        val deepLinkUri = when (payload.type) {
            NotificationType.CHECKIN ->
                "aekkim://checkin/${payload.referenceId}?isFullScreen=true"

            NotificationType.CHURN_REVIEW ->
                "aekkim://subscriptions/${payload.referenceId}"

            NotificationType.PROMO ->
                "aekkim://promotions/${payload.referenceId}"
        }.toUri()
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            deepLinkUri,
            this,
            MainActivity::class.java,
        )
        val notificationId = "${payload.type.name}_${payload.referenceId}".hashCode()
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val channelId = when (payload.type) {
            NotificationType.CHECKIN -> CHECK_IN_CHANNEL_ID
            NotificationType.CHURN_REVIEW,
            NotificationType.PROMO,
            -> GENERAL_NOTIFICATION_CHANNEL_ID
        }
        val category = when (payload.type) {
            NotificationType.CHECKIN -> NotificationCompat.CATEGORY_REMINDER
            NotificationType.CHURN_REVIEW -> NotificationCompat.CATEGORY_STATUS
            NotificationType.PROMO -> NotificationCompat.CATEGORY_RECOMMENDATION
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo_aekkim)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.logo_aekkim))
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(category)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun NotificationType.defaultTitle(): String =
        when (this) {
            NotificationType.CHECKIN -> "Check-in reminder"
            NotificationType.CHURN_REVIEW -> "Subscription reminder"
            NotificationType.PROMO -> "Promotion available"
        }

    private fun NotificationType.defaultBody(): String =
        when (this) {
            NotificationType.CHECKIN -> "Tap to open the check-in flow."
            NotificationType.CHURN_REVIEW -> "Tap to open this subscription."
            NotificationType.PROMO -> "Tap to view this promotion."
        }

    private data class NotificationPayload(
        val type: NotificationType,
        val referenceId: String,
        val title: String,
        val body: String,
    )

    private companion object {
        const val CHECK_IN_CHANNEL_ID = "aekkim_checkin"
        const val GENERAL_NOTIFICATION_CHANNEL_ID = "aekkim_cancel_review"
    }
}
