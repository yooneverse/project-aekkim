package com.ssafy.e106.feature.dashboard

import com.ssafy.e106.data.repository.PromotionType
import java.time.LocalDate

data class DashboardUiState(
    val subscriptions: List<SubscriptionCardItem> = emptyList(),
    val monthlyTotalAmount: Int = 0,
    val monthlyExpectedSavingAmount: Int = 0,
    val hasUnreadNotifications: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isEmpty: Boolean = false,
    val isEditMode: Boolean = false,
    val pendingDeletedSubscriptionIds: Set<Long> = emptySet(),
    val isApplyingEditChanges: Boolean = false,
    val usageReminderBanner: UsageReminderBannerState? = null,
    val pendingCheckinBanner: CheckinBannerState? = null,
    val pendingReviewCount: Int = 0,
    val pendingReviews: List<PendingReviewUiItem> = emptyList(),
    val selectedSubscriptionId: Long? = null,
    val subscriptionDetail: SubscriptionDetailUiModel? = null,
    val subscriptionDetailError: String? = null,
    val isDetailLoading: Boolean = false,
    val manualAddStep: ManualAddStep? = null,
    val manualAddQuery: String = "",
    val serviceOptions: List<DashboardServiceItem> = emptyList(),
    val serviceSearchResults: List<DashboardServiceItem> = emptyList(),
    val bundleOptions: List<DashboardBundleItem> = emptyList(),
    val bundleSearchResults: List<DashboardBundleItem> = emptyList(),
    val selectedService: DashboardServiceItem? = null,
    val selectedBundle: DashboardBundleItem? = null,
    val planOptions: List<DashboardPlanItem> = emptyList(),
    val selectedPlanId: Long? = null,
    val selectedBillingDay: Int? = null,
    val selectedBillingDate: LocalDate? = null,
    val editingSubscriptionId: Long? = null,
    val error: String? = null,
    val isStale: Boolean = false,
)

enum class ManualAddStep {
    SearchCatalog,
    SelectService,
    ConfigurePlanAndBillingDay,
    ConfigureBundleAndBillingDay,
}

data class SubscriptionCardItem(
    val subscriptionId: Long,
    val serviceId: Long,
    val servicePlanId: Long,
    val subscriptionType: String = "SINGLE",
    val bundleCode: String? = null,
    val serviceName: String,
    val planName: String? = null,
    val billingCycle: String? = null,
    val logoUrl: String? = null,
    val monthlyPrice: Int,
    val nextBillingDate: String?,
    val expectedSavingAmount: Int? = null,
    val savingLabel: String?,
    val coveredServices: List<DashboardServiceItem> = emptyList(),
    val nudgeMessage: String? = null,
)

data class CheckinBannerState(
    val subscriptionId: Long,
    val title: String,
    val description: String,
)

data class UsageReminderBannerState(
    val signature: String,
    val title: String,
    val candidateCount: Int,
    val targetSubscriptionId: Long,
)

data class PendingReviewUiItem(
    val reviewId: Long,
    val merchantName: String,
    val suggestedServiceName: String?,
    val monthlyAmount: Int,
    val billedAtLabel: String,
)

data class DashboardServiceItem(
    val serviceId: Long,
    val code: String,
    val name: String,
    val logoUrl: String? = null,
    val aliases: Set<String> = emptySet(),
)

data class DashboardPlanItem(
    val servicePlanId: Long,
    val planName: String,
    val billingCycle: String,
    val monthlyPrice: Int,
)

data class DashboardBundleItem(
    val code: String,
    val name: String,
    val planName: String,
    val billingCycle: String,
    val monthlyPrice: Int,
    val originalPrice: Int? = null,
    val logoUrl: String? = null,
    val includedServices: List<DashboardServiceItem> = emptyList(),
)

data class SubscriptionDetailUiModel(
    val subscriptionId: Long,
    val serviceName: String,
    val logoUrl: String? = null,
    val planName: String,
    val billingCycle: String? = null,
    val monthlyPrice: Int,
    val nextBillingDate: String? = null,
    val nextBillingDateLabel: String,
    val usageDaily: List<SubscriptionDetailUsagePoint> = emptyList(),
    val isUsageDailyLoading: Boolean = false,
    val usageDailyErrorMessage: String? = null,
    val recommendations: List<RecommendationUiItem> = emptyList(),
    val cancelGuideUrl: String? = null,
    val customerServicePhone: String? = null,
    val contactEmail: String? = null,
)

data class SubscriptionDetailUsagePoint(
    val usageDate: String,
    val usedMinutes: Int,
)

data class RecommendationUiItem(
    val promotionId: Long,
    val headline: String,
    val monthlySavingAmount: Int? = null,
    val billingCycle: String? = null,
    val promotionType: PromotionType? = null,
    val summary: String? = null,
    val savingsBadgeText: String? = null,
    val primaryReason: String? = null,
    val imageUrl: String? = null,
    val services: List<RecommendationServiceLogoUiItem> = emptyList(),
    val originalPriceLabel: String? = null,
    val discountPriceLabel: String? = null,
)

data class RecommendationServiceLogoUiItem(
    val serviceId: Long,
    val label: String,
    val code: String? = null,
    val logoUrl: String? = null,
)

data class SubscriptionUsageUiItem(
    val cycleLabel: String,
    val responseLabel: String,
    val tone: UsageTone,
)

enum class UsageTone {
    Positive,
    Neutral,
    Caution,
}

sealed class DashboardUiEffect {
    data class ShowToast(val message: String) : DashboardUiEffect()
    data class AutoNavigateToCheckIn(val subscriptionId: Long) : DashboardUiEffect()
}

sealed class DashboardIntent {
    data object LoadDashboard : DashboardIntent()
    data object RefreshDashboard : DashboardIntent()
    data object RetryLoad : DashboardIntent()
    data object RetrySubscriptionDetail : DashboardIntent()
    data object DismissUsageReminderBanner : DashboardIntent()
    data object ToggleEditMode : DashboardIntent()
    data object OpenAddFlow : DashboardIntent()
    data object CloseManualAddFlow : DashboardIntent()
    data class UpdateManualAddQuery(val query: String) : DashboardIntent()
    data class SelectService(val serviceId: Long) : DashboardIntent()
    data class SelectBundle(val bundleCode: String) : DashboardIntent()
    data class SelectPlan(val servicePlanId: Long) : DashboardIntent()
    data class SelectBillingDay(val day: Int) : DashboardIntent()
    data class SelectBillingDate(val date: LocalDate) : DashboardIntent()
    data object SubmitManualAdd : DashboardIntent()
    data class OpenSubscriptionDetail(val subscriptionId: Long) : DashboardIntent()
    data object CloseSubscriptionDetail : DashboardIntent()
    data class StartEditSubscription(val subscriptionId: Long) : DashboardIntent()
    data class DeleteSubscription(val subscriptionId: Long) : DashboardIntent()
}
