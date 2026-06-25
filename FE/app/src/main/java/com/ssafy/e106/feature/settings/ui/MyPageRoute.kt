package com.ssafy.e106.feature.settings.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e106.core.notification.areAppNotificationsEnabled
import com.ssafy.e106.feature.settings.MyPageIntent
import com.ssafy.e106.feature.settings.MyPageUiEffect
import com.ssafy.e106.feature.settings.MyPageViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MyPageRoute(
    onNavigateToDashboard: () -> Unit,
    onNavigateToInsight: () -> Unit,
    onNavigateToPromotionList: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToServiceTerms: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToMarketingConsent: () -> Unit,
    viewModel: MyPageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val refreshNotificationPermission = {
        viewModel.onIntent(
            MyPageIntent.UpdateAppNotificationPermission(
                context.areAppNotificationsEnabled(),
            ),
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is MyPageUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                is MyPageUiEffect.OpenExternalLink -> {
                    runCatching { uriHandler.openUri(effect.url) }
                        .onFailure {
                            Toast.makeText(context, "링크를 열 수 없어요.", Toast.LENGTH_SHORT).show()
                        }
                }

                is MyPageUiEffect.NavigateToOnboarding -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    onNavigateToOnboarding()
                }

                MyPageUiEffect.NavigateToDeleteAccount -> onNavigateToDeleteAccount()
                MyPageUiEffect.NavigateToServiceTerms -> onNavigateToServiceTerms()
                MyPageUiEffect.NavigateToPrivacyPolicy -> onNavigateToPrivacyPolicy()
                MyPageUiEffect.NavigateToMarketingConsent -> onNavigateToMarketingConsent()
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.onIntent(MyPageIntent.LoadProfile)
        refreshNotificationPermission()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    MyPageScreen(
        uiState = uiState,
        onRetryLoad = { viewModel.onIntent(MyPageIntent.RetryLoad) },
        onNavigateToDashboard = onNavigateToDashboard,
        onNavigateToInsight = onNavigateToInsight,
        onNavigateToPromotionList = onNavigateToPromotionList,
        onCheckinAlertToggle = { enabled ->
            viewModel.onIntent(MyPageIntent.ToggleCheckinAlert(enabled))
        },
        onPromoAlertToggle = { enabled ->
            viewModel.onIntent(MyPageIntent.TogglePromoAlert(enabled))
        },
        onOptionalConsentToggle = { agreed ->
            viewModel.onIntent(MyPageIntent.ToggleOptionalConsent(agreed))
        },
        onTermsClick = { viewModel.onIntent(MyPageIntent.OpenTerms) },
        onPrivacyPolicyClick = { viewModel.onIntent(MyPageIntent.OpenPrivacyPolicy) },
        onMarketingConsentClick = { viewModel.onIntent(MyPageIntent.OpenMarketingConsent) },
        onOpenNotificationSettings = {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
                .onFailure {
                    Toast.makeText(context, "알림 설정 화면을 열지 못했어요.", Toast.LENGTH_SHORT).show()
                }
        },
        onLogoutClick = { viewModel.onIntent(MyPageIntent.LogoutClicked) },
        onDismissLogoutDialog = { viewModel.onIntent(MyPageIntent.DismissLogoutDialog) },
        onConfirmLogout = { viewModel.onIntent(MyPageIntent.ConfirmLogout) },
        onDeleteAccountClick = { viewModel.onIntent(MyPageIntent.DeleteAccountClicked) },
    )
}
