package com.ssafy.e106.feature.notification.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e106.feature.notification.NotificationIntent
import com.ssafy.e106.feature.notification.NotificationUiEffect
import com.ssafy.e106.feature.notification.NotificationViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun NotificationRoute(
    onNavigateBack: () -> Unit,
    onNavigateToCheckIn: (Long) -> Unit,
    onNavigateToSubscriptionDetail: (Long) -> Unit,
    onNavigateToPromotionDetail: (Long) -> Unit,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is NotificationUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                is NotificationUiEffect.NavigateToCheckIn -> {
                    onNavigateToCheckIn(effect.subscriptionId)
                }

                is NotificationUiEffect.NavigateToPromotionDetail -> {
                    onNavigateToPromotionDetail(effect.promotionId)
                }

                is NotificationUiEffect.NavigateToSubscriptionDetail -> {
                    onNavigateToSubscriptionDetail(effect.subscriptionId)
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.onIntent(NotificationIntent.LoadNotifications)
    }

    NotificationScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRefresh = { viewModel.onIntent(NotificationIntent.RefreshNotifications) },
        onRetryLoad = { viewModel.onIntent(NotificationIntent.RetryLoad) },
        onNotificationClick = { notificationId ->
            viewModel.onIntent(NotificationIntent.OpenNotification(notificationId))
        },
        onNotificationDelete = { notificationId ->
            viewModel.onIntent(NotificationIntent.DeleteNotification(notificationId))
        },
        onDeleteAllClick = {
            viewModel.onIntent(NotificationIntent.DeleteAllNotifications)
        },
    )
}
