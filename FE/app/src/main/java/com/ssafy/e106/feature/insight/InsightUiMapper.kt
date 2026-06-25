package com.ssafy.e106.feature.insight

import com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageDailyPointResponse
import com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageDailyResponse
import com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageReportItemResponse
import com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageReportResponse
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun toInsightScreenState(
    report: SubscriptionUsageReportResponse,
    daily: SubscriptionUsageDailyResponse,
): InsightScreenState {
    if (report.items.isEmpty() && daily.items.isEmpty()) {
        return InsightScreenState.Empty
    }

    return InsightScreenState.Success(
        report = InsightReportUiModel(
            summary = InsightSummaryUiModel(
                windowDays = report.summary.windowDays,
                totalUsedMinutes = report.summary.totalUsedMinutes,
                activeSubscriptionCount = report.summary.activeSubscriptionCount,
                lowUsageSubscriptionCount = report.summary.lowUsageSubscriptionCount,
                mostUsedSubscriptionName = report.summary.mostUsedSubscriptionName,
                mostUsedSubscriptionMinutes = report.summary.mostUsedSubscriptionMinutes,
            ),
            dailyFlow = daily.items.toDailyFlow(report.summary.windowDays),
            relatedInsights = report.relatedInsights.map { it.message }.filter { it.isNotBlank() },
            items = report.items
                .map { item -> item.toInsightItem() }
                .sortedWith(
                    compareByDescending<InsightSubscriptionItemUiModel> { it.totalUsedMinutes }
                        .thenByDescending { it.usedDays }
                        .thenByDescending { it.monthlyPrice },
                ),
        ),
    )
}

private fun List<SubscriptionUsageDailyPointResponse>.toDailyFlow(
    days: Int,
): List<InsightDailyUsageUiModel> {
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val today = LocalDate.now()
    val valuesByDate = associate { point ->
        LocalDate.parse(point.usageDate, formatter) to point.totalUsedMinutes
    }

    return (days - 1 downTo 0).map { offset ->
        val targetDate = today.minusDays(offset.toLong())
        InsightDailyUsageUiModel(
            dateLabel = targetDate.format(formatter),
            usedMinutes = valuesByDate[targetDate] ?: 0,
        )
    }
}

private fun SubscriptionUsageReportItemResponse.toInsightItem(): InsightSubscriptionItemUiModel {
    return InsightSubscriptionItemUiModel(
        subscriptionId = subscriptionId,
        serviceName = serviceName,
        planName = planName,
        subscriptionType = subscriptionType,
        bundleCode = bundleCode,
        category = category,
        logoUrl = logoUrl,
        monthlyPrice = monthlyPrice,
        totalUsedMinutes = totalUsedMinutes,
        usedDays = usedDays,
        lastUsedDateLabel = lastUsedDate
            ?.let { parsed -> runCatching { LocalDate.parse(parsed) }.getOrNull() }
            ?.let { date -> "${date.monthValue}월 ${date.dayOfMonth}일" },
        hourlyCost = hourlyCost,
        nudgeMessage = nudgeMessage,
    )
}

internal fun formatInsightMinutes(totalUsedMinutes: Int): String {
    val hours = totalUsedMinutes / 60
    val minutes = totalUsedMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
        hours > 0 -> "${hours}시간"
        else -> "${minutes}분"
    }
}

internal fun formatInsightWon(amount: Int): String {
    return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원"
}
