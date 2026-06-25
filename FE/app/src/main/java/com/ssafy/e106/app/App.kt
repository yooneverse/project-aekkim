package com.ssafy.e106.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import com.ssafy.e106.BuildConfig
import com.ssafy.e106.data.repository.NotificationRepository
import com.ssafy.e106.data.repository.TokenRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject
    lateinit var tokenRepository: TokenRepository

    @Inject
    lateinit var notificationRepository: NotificationRepository

    override fun onCreate() {
        super.onCreate()
        Log.d(APP_INIT_TAG, "App.onCreate started")
        initializeKakaoSdk()
        createNotificationChannels()
        Log.d(APP_INIT_TAG, "Notification channels created")
        tokenRepository.initialize()
        Log.d(APP_INIT_TAG, "Token repository initialized")
        notificationRepository.syncFcmTokenOnAppStart()
    }

    private fun initializeKakaoSdk() {
        val nativeAppKey = BuildConfig.KAKAO_NATIVE_APP_KEY
        if (nativeAppKey.isBlank() || isPlaceholderNativeAppKey(nativeAppKey)) {
            Log.w(APP_INIT_TAG, "Kakao SDK init skipped: KAKAO_NATIVE_APP_KEY is not configured")
            return
        }

        KakaoSdk.init(this, nativeAppKey)
        Log.d(APP_INIT_TAG, "Kakao SDK initialized")
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHECK_IN_CHANNEL_ID,
                "체크인 알림",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "거비그 이용 여부 확인 알림"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CANCEL_REVIEW_CHANNEL_ID,
                "해지 검토 알림",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "프로모션 및 해지 검토 안내 알림"
            },
        )
    }

    private companion object {
        const val APP_INIT_TAG = "APP_INIT"
        const val CHECK_IN_CHANNEL_ID = "aekkim_checkin"
        const val CANCEL_REVIEW_CHANNEL_ID = "aekkim_cancel_review"
    }
}

private fun isPlaceholderNativeAppKey(value: String): Boolean {
    return value.equals("your_kakao_native_key_here", ignoreCase = true) ||
        value.equals("kakao_native_app_key_here", ignoreCase = true) ||
        value.startsWith("your_", ignoreCase = true)
}
