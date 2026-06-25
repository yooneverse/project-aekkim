package com.ssafy.e106.feature.insight

data class InsightUiState(
    val screenState: InsightScreenState = InsightScreenState.Loading,
)

sealed interface InsightScreenState {
    data object Loading : InsightScreenState
    data class Success(val report: InsightReportUiModel) : InsightScreenState
    data object Empty : InsightScreenState
    data class Error(val message: String) : InsightScreenState
}

data class InsightReportUiModel(
    val summary: InsightSummaryUiModel,
    val dailyFlow: List<InsightDailyUsageUiModel>,
    val relatedInsights: List<String>,
    val items: List<InsightSubscriptionItemUiModel>,
)

data class InsightSummaryUiModel(
    val windowDays: Int,
    val totalUsedMinutes: Int,
    val activeSubscriptionCount: Int,
    val lowUsageSubscriptionCount: Int,
    val mostUsedSubscriptionName: String? = null,
    val mostUsedSubscriptionMinutes: Int? = null,
)

data class InsightDailyUsageUiModel(
    val dateLabel: String,
    val usedMinutes: Int,
)

data class InsightSubscriptionItemUiModel(
    val subscriptionId: Long,
    val serviceName: String,
    val planName: String,
    val subscriptionType: String,
    val bundleCode: String? = null,
    val category: String? = null,
    val logoUrl: String? = null,
    val monthlyPrice: Int,
    val totalUsedMinutes: Int,
    val usedDays: Int,
    val lastUsedDateLabel: String? = null,
    val hourlyCost: Int? = null,
    val nudgeMessage: String? = null,
)

sealed interface InsightUiEffect

sealed interface InsightIntent {
    data object LoadInsights : InsightIntent
    data object RetryLoad : InsightIntent
}
