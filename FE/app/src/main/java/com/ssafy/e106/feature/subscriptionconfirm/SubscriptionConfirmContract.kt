package com.ssafy.e106.feature.subscriptionconfirm

data class SubscriptionConfirmUiState(
    val nickname: String = FALLBACK_NICKNAME,
    val detectedSubscriptions: List<DetectedSubscriptionItem> = emptyList(),
    val pendingReviewSummary: PendingReviewSummaryItem? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isCompletingLocalSessionOnly: Boolean = false,
) {
    val visibleDetectedSubscriptions: List<DetectedSubscriptionItem>
        get() = detectedSubscriptions.filterNot { item -> item.excludedLocallyInSession }

    val isDetectedSubscriptionsEmpty: Boolean
        get() = visibleDetectedSubscriptions.isEmpty()
}

data class DetectedSubscriptionItem(
    val reviewId: Long,
    val subscriptionType: String,
    val serviceId: Long? = null,
    val servicePlanId: Long? = null,
    val bundleCode: String? = null,
    val serviceName: String,
    val monthlyAmount: Int,
    val billedAtLabel: String,
    val billingDay: Int,
    val logoUrl: String? = null,
    val excludedLocallyInSession: Boolean = false,
)

data class PendingReviewSummaryItem(
    val count: Int,
    val suggestedServiceNamesLabel: String? = null,
)

sealed class SubscriptionConfirmUiEffect {
    data object NavigateToDashboard : SubscriptionConfirmUiEffect()
    data object NavigateToManualMapping : SubscriptionConfirmUiEffect()
    data class ShowToast(val message: String) : SubscriptionConfirmUiEffect()
}

sealed class SubscriptionConfirmIntent {
    data object Load : SubscriptionConfirmIntent()
    data object RetryLoad : SubscriptionConfirmIntent()
    data class ExcludeDetectedSubscription(val reviewId: Long) : SubscriptionConfirmIntent()
    data object OpenPendingReview : SubscriptionConfirmIntent()
    data object ConfirmDetectedSubscriptions : SubscriptionConfirmIntent()
}

private const val FALLBACK_NICKNAME = "회원"
