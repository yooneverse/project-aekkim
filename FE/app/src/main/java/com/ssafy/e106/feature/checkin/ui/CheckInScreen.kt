package com.ssafy.e106.feature.checkin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.e106.core.ui.component.image.ServiceLogoBadge
import com.ssafy.e106.core.ui.component.feedback.AppErrorState
import com.ssafy.e106.core.ui.component.feedback.AppLoadingState
import com.ssafy.e106.core.ui.theme.LocalSpacing
import com.ssafy.e106.core.ui.theme.SurfaceMuted
import com.ssafy.e106.feature.checkin.CheckInResponse
import com.ssafy.e106.feature.checkin.CheckInSubmitError
import com.ssafy.e106.feature.checkin.CheckInUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    uiState: CheckInUiState,
    onSelectResponse: (CheckInResponse) -> Unit,
    onRetrySubmit: () -> Unit,
    onRetryLoad: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = spacing.screenHorizontalPadding),
        ) {
            when {
                uiState.isLoading -> {
                    AppLoadingState(
                        title = "체크인을 준비하는 중이에요",
                        description = "구독 정보를 확인하고 있어요",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                uiState.error != null -> {
                    AppErrorState(
                        title = "체크인을 불러오지 못했어요",
                        description = uiState.error,
                        onRetryClick = onRetryLoad,
                        secondaryActionText = "대시보드 홈으로 이동",
                        onSecondaryActionClick = onNavigateHome,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    CheckInContent(
                        serviceName = uiState.serviceName,
                        logoUrl = uiState.logoUrl,
                        submittingResponse = uiState.submittingResponse,
                        submitError = uiState.submitError,
                        onSelectResponse = onSelectResponse,
                        onRetrySubmit = onRetrySubmit,
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckInContent(
    serviceName: String,
    logoUrl: String?,
    submittingResponse: CheckInResponse?,
    submitError: CheckInSubmitError?,
    onSelectResponse: (CheckInResponse) -> Unit,
    onRetrySubmit: () -> Unit,
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            ServiceBadge(
                serviceName = serviceName,
                logoUrl = logoUrl,
            )

            Spacer(modifier = Modifier.height(spacing.space5))

            Text(
                text = "$serviceName 결제일이\n다가와요.",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 36.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(spacing.space3))

            Text(
                text = "이번 달 얼마나 보셨나요?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.space8),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            submitError?.let { error ->
                SubmitErrorCard(
                    submitError = error,
                    isSubmitting = submittingResponse != null,
                    onRetrySubmit = onRetrySubmit,
                )
            }

            CheckInResponse.entries.forEach { response ->
                CheckInResponseButton(
                    response = response,
                    isLoading = submittingResponse == response,
                    enabled = submittingResponse == null,
                    onClick = { onSelectResponse(response) },
                )
            }
        }
    }
}

@Composable
private fun SubmitErrorCard(
    submitError: CheckInSubmitError,
    isSubmitting: Boolean,
    onRetrySubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Text(
                text = "체크인 제출이 완료되지 않았어요",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = submitError.message,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (submitError.canRetry && submitError.retryResponse != null) {
                OutlinedButton(
                    onClick = onRetrySubmit,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "다시 시도")
                }
            }
        }
    }
}

@Composable
private fun ServiceBadge(
    serviceName: String,
    logoUrl: String?,
    modifier: Modifier = Modifier,
) {
    ServiceLogoBadge(
        serviceName = serviceName,
        logoUrl = logoUrl,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        border = null,
        fallbackTextColor = MaterialTheme.colorScheme.onPrimary,
        fallbackFontWeight = FontWeight.ExtraBold,
        contentPadding = 10.dp,
    )
}

@Composable
private fun CheckInResponseButton(
    response: CheckInResponse,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceMuted,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = SurfaceMuted,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = null,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = response.label,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = response.emoji,
                    fontSize = 22.sp,
                )
            }
        }
    }
}
