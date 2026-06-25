package com.ssafy.e106.feature.analysis

import android.app.usage.UsageStatsManager
import android.content.Context
import com.ssafy.e106.feature.analysis.model.AppUsageSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class UsageStatsCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionChecker: UsagePermissionChecker,
) {
    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    fun collectAll(packageNames: Collection<String>): List<AppUsageSnapshot> {
        return packageNames.map { packageName -> collect(packageName) }
    }

    private fun collect(packageName: String): AppUsageSnapshot {
        val now = System.currentTimeMillis()
        val zoneId = ZoneId.systemDefault()

        if (!permissionChecker.isGranted()) {
            return AppUsageSnapshot(
                packageName = packageName,
                usage7dMs = 0L,
                usage30dMs = 0L,
                dailyUsageMinutesByDate = emptyMap(),
                lastUsedEpochMs = null,
                permissionGranted = false,
                reason = "permission_denied",
                queriedAtEpochMs = now,
                timezone = zoneId.id,
            )
        }

        val start7d = daysAgoStartMillis(days = 7, zoneId = zoneId)
        val start30d = daysAgoStartMillis(days = 30, zoneId = zoneId)

        val usage7d = queryForegroundTime(packageName = packageName, start = start7d, end = now)
        val usage30d = queryForegroundTime(packageName = packageName, start = start30d, end = now)
        val dailyUsageMinutes = queryDailyForegroundMinutes(
            packageName = packageName,
            start = start30d,
            end = now,
            zoneId = zoneId,
        )
        val lastUsed = queryLastUsed(packageName = packageName, start = start30d, end = now)

        return AppUsageSnapshot(
            packageName = packageName,
            usage7dMs = usage7d,
            usage30dMs = usage30d,
            dailyUsageMinutesByDate = dailyUsageMinutes,
            lastUsedEpochMs = lastUsed,
            permissionGranted = true,
            reason = if (usage7d == 0L && usage30d == 0L && lastUsed == null) "no_history" else null,
            queriedAtEpochMs = now,
            timezone = zoneId.id,
        )
    }

    private fun daysAgoStartMillis(days: Long, zoneId: ZoneId): Long {
        return LocalDate.now(zoneId)
            .minusDays(days)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun queryForegroundTime(packageName: String, start: Long, end: Long): Long {
        val usageByPackage = usageStatsManager.queryAndAggregateUsageStats(start, end)
        return usageByPackage[packageName]?.totalTimeInForeground ?: 0L
    }

    private fun queryLastUsed(packageName: String, start: Long, end: Long): Long? {
        val usageByPackage = usageStatsManager.queryAndAggregateUsageStats(start, end)
        val lastUsed = usageByPackage[packageName]?.lastTimeUsed ?: 0L
        return if (lastUsed > 0L) lastUsed else null
    }

    private fun queryDailyForegroundMinutes(
        packageName: String,
        start: Long,
        end: Long,
        zoneId: ZoneId,
    ): Map<LocalDate, Int> {
        return usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            .asSequence()
            .filter { usage -> usage.packageName == packageName }
            .groupBy { usage ->
                java.time.Instant.ofEpochMilli(usage.firstTimeStamp)
                    .atZone(zoneId)
                    .toLocalDate()
            }
            .mapValues { (_, usages) ->
                (usages.sumOf { usage -> usage.totalTimeInForeground } / 60_000L).toInt()
            }
            .filterValues { minutes -> minutes >= 1 }
    }
}
