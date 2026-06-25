package com.ssafy.e106.feature.analysis.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.e106.feature.analysis.viewmodel.AnalysisResultUiEffect
import com.ssafy.e106.feature.analysis.viewmodel.AnalysisResultViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AnalysisResultRoute(
    onNavigateToDashboard: () -> Unit,
    onNavigateToManualMapping: () -> Unit,
    viewModel: AnalysisResultViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                AnalysisResultUiEffect.NavigateToDashboard -> onNavigateToDashboard()
                AnalysisResultUiEffect.NavigateToManualMapping -> onNavigateToManualMapping()
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.resolveDestination()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = "분석 결과를 정리하고 있어요.",
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "확인이 필요한 결제가 있는지 마지막으로 확인하는 중입니다.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
