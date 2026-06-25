package com.ssafy.e106.feature.analysis.model

import java.time.LocalDate

data class AppUsageSnapshot(
    val packageName: String,
    val usage7dMs: Long,
    val usage30dMs: Long,
    val dailyUsageMinutesByDate: Map<LocalDate, Int> = emptyMap(),
    val lastUsedEpochMs: Long?,
    val permissionGranted: Boolean,
    val reason: String?,
    val queriedAtEpochMs: Long,
    val timezone: String,
)
