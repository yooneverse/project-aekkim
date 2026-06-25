package com.ssafy.e106.feature.checkin.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e106.feature.checkin.CheckInIntent
import com.ssafy.e106.feature.checkin.CheckInUiEffect
import com.ssafy.e106.feature.checkin.CheckInViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CheckInRoute(
    subscriptionId: String,
    isFullScreen: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToFollowUp: (String) -> Unit,
    viewModel: CheckInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler(enabled = true) {
        if (isFullScreen) {
            onNavigateHome()
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(subscriptionId) {
        val parsedId = subscriptionId.toLongOrNull() ?: -1L
        viewModel.onIntent(CheckInIntent.Load(parsedId))
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is CheckInUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                CheckInUiEffect.NavigateBack -> {
                    if (isFullScreen) {
                        onNavigateHome()
                    } else {
                        onNavigateBack()
                    }
                }

                is CheckInUiEffect.NavigateToFollowUp -> {
                    onNavigateToFollowUp(effect.subscriptionId.toString())
                }
            }
        }
    }

    CheckInScreen(
        uiState = uiState,
        onSelectResponse = { response ->
            viewModel.onIntent(CheckInIntent.Submit(response))
        },
        onRetrySubmit = {
            viewModel.onIntent(CheckInIntent.RetrySubmit)
        },
        onRetryLoad = { viewModel.onIntent(CheckInIntent.RetryLoad) },
        onNavigateBack = {
            if (isFullScreen) {
                onNavigateHome()
            } else {
                onNavigateBack()
            }
        },
        onNavigateHome = onNavigateHome,
    )
}
