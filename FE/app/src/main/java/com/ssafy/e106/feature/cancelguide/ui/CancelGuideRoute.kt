package com.ssafy.e106.feature.cancelguide.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e106.feature.cancelguide.CancelGuideIntent
import com.ssafy.e106.feature.cancelguide.CancelGuideUiEffect
import com.ssafy.e106.feature.cancelguide.CancelGuideViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CancelGuideRoute(
    subscriptionId: String,
    onNavigateBack: () -> Unit,
    viewModel: CancelGuideViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(subscriptionId) {
        viewModel.onIntent(
            CancelGuideIntent.Load(subscriptionId = subscriptionId.toLongOrNull() ?: -1L),
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is CancelGuideUiEffect.OpenUri -> {
                    runCatching { context.openExternalUri(effect.uri) }
                        .onFailure {
                            Toast.makeText(context, "연결할 수 없어요.", Toast.LENGTH_SHORT).show()
                        }
                }

                is CancelGuideUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    CancelGuideScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRetryLoad = { viewModel.onIntent(CancelGuideIntent.RetryLoad) },
        onOpenCancelGuideLink = { viewModel.onIntent(CancelGuideIntent.OpenCancelGuideLink) },
        onCallCustomerService = { viewModel.onIntent(CancelGuideIntent.CallCustomerService) },
        onSendContactEmail = { viewModel.onIntent(CancelGuideIntent.SendContactEmail) },
    )
}

private fun Context.openExternalUri(uri: String) {
    val parsedUri = Uri.parse(uri)
    val intent = when (parsedUri.scheme?.lowercase()) {
        "tel" -> Intent(Intent.ACTION_DIAL, parsedUri)
        "mailto" -> Intent(Intent.ACTION_SENDTO, parsedUri)
        else -> Intent(Intent.ACTION_VIEW, parsedUri)
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    startActivity(intent)
}
