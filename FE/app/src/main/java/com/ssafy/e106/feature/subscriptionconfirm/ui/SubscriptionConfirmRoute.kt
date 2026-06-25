package com.ssafy.e106.feature.subscriptionconfirm.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e106.feature.subscriptionconfirm.SubscriptionConfirmIntent
import com.ssafy.e106.feature.subscriptionconfirm.SubscriptionConfirmUiEffect
import com.ssafy.e106.feature.subscriptionconfirm.SubscriptionConfirmViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SubscriptionConfirmRoute(
    onNavigateToDashboard: () -> Unit,
    onNavigateToManualMapping: () -> Unit,
    viewModel: SubscriptionConfirmViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler(enabled = true) {
        // Prevent returning to the analysis flow after the confirmation step.
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                SubscriptionConfirmUiEffect.NavigateToDashboard -> onNavigateToDashboard()
                SubscriptionConfirmUiEffect.NavigateToManualMapping -> onNavigateToManualMapping()
                is SubscriptionConfirmUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.onIntent(SubscriptionConfirmIntent.Load)
    }

    SubscriptionConfirmScreen(
        uiState = uiState.value,
        onRetryLoad = { viewModel.onIntent(SubscriptionConfirmIntent.RetryLoad) },
        onExcludeDetectedSubscription = { reviewId ->
            viewModel.onIntent(SubscriptionConfirmIntent.ExcludeDetectedSubscription(reviewId))
        },
        onOpenPendingReview = { viewModel.onIntent(SubscriptionConfirmIntent.OpenPendingReview) },
        onConfirmDetectedSubscriptions = {
            viewModel.onIntent(SubscriptionConfirmIntent.ConfirmDetectedSubscriptions)
        },
    )
}
