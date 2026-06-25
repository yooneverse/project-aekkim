package com.ssafy.e106.feature.settings.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e106.feature.settings.DeleteAccountIntent
import com.ssafy.e106.feature.settings.DeleteAccountUiEffect
import com.ssafy.e106.feature.settings.DeleteAccountViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DeleteAccountRoute(
    onNavigateBack: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: DeleteAccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is DeleteAccountUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                is DeleteAccountUiEffect.NavigateToOnboarding -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    onNavigateToOnboarding()
                }

                DeleteAccountUiEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    DeleteAccountScreen(
        uiState = uiState,
        onNextStep = { viewModel.onIntent(DeleteAccountIntent.NextStep) },
        onPreviousStep = { viewModel.onIntent(DeleteAccountIntent.PreviousStep) },
        onToggleFinalCheck = { agreed -> viewModel.onIntent(DeleteAccountIntent.ToggleFinalCheck(agreed)) },
        onConfirmDelete = { viewModel.onIntent(DeleteAccountIntent.ConfirmDelete) },
    )
}
