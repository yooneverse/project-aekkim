package com.ssafy.e106.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Check-in 노출 정책 저장소.
 *
 * 현재 FE/BE 간 pending check-in 조회 API가 아직 연결되지 않아
 * 대시보드 배너/자동 풀스크린 진입은 비활성화한다.
 * 실제 API가 붙으면 getPendingCheckIns()에 서버 조회를 연결하면 된다.
 */
@Singleton
class CheckInPolicyRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getPendingCheckIns(): List<PendingCheckIn> {
        return emptyList()
    }

    fun getAutoFullScreenTarget(): PendingCheckIn? {
        if (hasShownFullScreenToday()) return null
        return getPendingCheckIns().firstOrNull { checkin ->
            checkin.isDueToday && checkin.isPending
        }
    }

    fun markFullScreenShown() {
        prefs.edit()
            .putString(KEY_LAST_FULLSCREEN_DATE, todayString())
            .apply()
    }

    fun hasShownFullScreenToday(): Boolean {
        val lastShownDate = prefs.getString(KEY_LAST_FULLSCREEN_DATE, null)
        return lastShownDate == todayString()
    }

    fun getPendingCheckinForBanner(): PendingCheckIn? {
        return getPendingCheckIns().firstOrNull { checkin -> checkin.isPending }
    }

    private fun todayString(): String {
        return LocalDate.now().format(DATE_FORMATTER)
    }

    private companion object {
        const val PREFS_NAME = "checkin_policy_prefs"
        const val KEY_LAST_FULLSCREEN_DATE = "last_fullscreen_date"

        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}

data class PendingCheckIn(
    val subscriptionId: Long,
    val serviceName: String,
    val isDueToday: Boolean,
    val isPending: Boolean,
)
