package com.ssafy.e106.feature.dashboard.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e106.feature.dashboard.DashboardIntent
import com.ssafy.e106.feature.dashboard.DashboardUiEffect
import com.ssafy.e106.feature.dashboard.DashboardViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DashboardRoute(
    onNavigateToPromotionList: () -> Unit,
    onNavigateToInsight: (Long?) -> Unit,
    onNavigateToPromotionDetail: (Long) -> Unit,
    pendingEditSubscriptionId: Long?,
    onConsumePendingEditSubscription: (Long) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCheckIn: (Long) -> Unit,
    onNavigateToCancelGuide: (Long) -> Unit,
    onNavigateToManualMapping: () -> Unit,
    onNavigateToMyPage: () -> Unit,
    onAutoNavigateToFullScreenCheckIn: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is DashboardUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                is DashboardUiEffect.AutoNavigateToCheckIn -> {
                    onAutoNavigateToFullScreenCheckIn(effect.subscriptionId)
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.onIntent(DashboardIntent.LoadDashboard)
    }

    LaunchedEffect(pendingEditSubscriptionId) {
        val targetSubscriptionId = pendingEditSubscriptionId ?: return@LaunchedEffect
        viewModel.onIntent(DashboardIntent.StartEditSubscription(targetSubscriptionId))
        onConsumePendingEditSubscription(targetSubscriptionId)
    }

    DashboardScreen(
        uiState = uiState,
        onRefresh = { viewModel.onIntent(DashboardIntent.RefreshDashboard) },
        onRetryLoad = { viewModel.onIntent(DashboardIntent.RetryLoad) },
        onRetrySubscriptionDetail = { viewModel.onIntent(DashboardIntent.RetrySubscriptionDetail) },
        onDismissUsageReminderBanner = { viewModel.onIntent(DashboardIntent.DismissUsageReminderBanner) },
        onToggleEditMode = { viewModel.onIntent(DashboardIntent.ToggleEditMode) },
        onOpenAddFlow = { viewModel.onIntent(DashboardIntent.OpenAddFlow) },
        onDismissManualAddFlow = { viewModel.onIntent(DashboardIntent.CloseManualAddFlow) },
        onUpdateManualAddQuery = { query -> viewModel.onIntent(DashboardIntent.UpdateManualAddQuery(query)) },
        onSelectService = { serviceId -> viewModel.onIntent(DashboardIntent.SelectService(serviceId)) },
        onSelectBundle = { bundleCode -> viewModel.onIntent(DashboardIntent.SelectBundle(bundleCode)) },
        onSelectPlan = { planId -> viewModel.onIntent(DashboardIntent.SelectPlan(planId)) },
        onSelectBillingDay = { day -> viewModel.onIntent(DashboardIntent.SelectBillingDay(day)) },
        onSelectBillingDate = { date -> viewModel.onIntent(DashboardIntent.SelectBillingDate(date)) },
        onSubmitManualAdd = { viewModel.onIntent(DashboardIntent.SubmitManualAdd) },
        onOpenSubscriptionDetail = { subscriptionId ->
            viewModel.onIntent(DashboardIntent.OpenSubscriptionDetail(subscriptionId))
        },
        onCloseSubscriptionDetail = { viewModel.onIntent(DashboardIntent.CloseSubscriptionDetail) },
        onStartEditSubscription = { subscriptionId ->
            viewModel.onIntent(DashboardIntent.StartEditSubscription(subscriptionId))
        },
        onDeleteSubscription = { subscriptionId ->
            viewModel.onIntent(DashboardIntent.DeleteSubscription(subscriptionId))
        },
        onOpenPromotionList = onNavigateToPromotionList,
        onOpenInsight = { onNavigateToInsight(null) },
        onOpenUsageReminderInsight = { subscriptionId -> onNavigateToInsight(subscriptionId) },
        onOpenPromotionDetail = onNavigateToPromotionDetail,
        onOpenNotifications = onNavigateToNotifications,
        onOpenCheckIn = onNavigateToCheckIn,
        onOpenCancelGuide = onNavigateToCancelGuide,
        onOpenManualMapping = onNavigateToManualMapping,
        onOpenMyPage = onNavigateToMyPage,
    )
}
