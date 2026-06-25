package com.ssafy.e106.feature.analysis.model

data class AnalysisUiState(
    val permissionState: UsagePermissionState = UsagePermissionState.UNKNOWN,
    val usageItems: List<AppUsageSnapshot> = emptyList(),
    val isCollecting: Boolean = false,
    val currentStep: AnalysisStep = AnalysisStep.FetchingPayments,
    val completedSteps: Set<AnalysisStep> = emptySet(),
    val savingAmount: Int = 0,
)

enum class AnalysisStep {
    FetchingPayments,
    MatchingBenefits,
    BuildingReport,
}

sealed class AnalysisIntent {
    data object Entered : AnalysisIntent()
    data object OpenSettingsClicked : AnalysisIntent()
    data object RecheckPermissionOnResume : AnalysisIntent()
    data object ContinueClicked : AnalysisIntent()
}

sealed class AnalysisUiEffect {
    data object OpenUsageAccessSettings : AnalysisUiEffect()
    data object NavigateToSubscriptionConfirm : AnalysisUiEffect()
}
