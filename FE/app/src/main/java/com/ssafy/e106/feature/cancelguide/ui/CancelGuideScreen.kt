package com.ssafy.e106.feature.cancelguide.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.ssafy.e106.core.ui.component.button.AppButton
import com.ssafy.e106.core.ui.component.card.AppCard
import com.ssafy.e106.core.ui.component.feedback.AppErrorState
import com.ssafy.e106.core.ui.component.feedback.AppLoadingState
import com.ssafy.e106.core.ui.component.service.ServiceBrandIcon
import com.ssafy.e106.core.ui.model.ButtonSize
import com.ssafy.e106.core.ui.model.ButtonVariant
import com.ssafy.e106.core.ui.theme.LocalSpacing
import com.ssafy.e106.feature.cancelguide.CancelGuideUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CancelGuideScreen(
    uiState: CancelGuideUiState,
    onNavigateBack: () -> Unit,
    onRetryLoad: () -> Unit,
    onOpenCancelGuideLink: () -> Unit,
    onCallCustomerService: () -> Unit,
    onSendContactEmail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val topBarTitle = uiState.serviceName.takeIf { it.isNotBlank() }?.let { "$it 해지 방법" } ?: "해지 방법"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
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
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = spacing.screenHorizontalPadding,
                        end = spacing.screenHorizontalPadding,
                        top = spacing.space3,
                        bottom = spacing.space5,
                    ),
            ) {
                AppButton(
                    text = "나중에 할게요",
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Ghost,
                    size = ButtonSize.Large,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = spacing.screenHorizontalPadding),
        ) {
            when {
                uiState.isLoading -> {
                    AppLoadingState(
                        title = "해지 지원 정보를 불러오는 중이에요.",
                        description = "서비스별 안내와 연락 수단을 확인하고 있어요.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                uiState.error != null -> {
                    AppErrorState(
                        title = "해지 지원 정보를 불러오지 못했어요.",
                        description = uiState.error,
                        onRetryClick = onRetryLoad,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = spacing.space4,
                            bottom = spacing.space4,
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.space4),
                    ) {
                        item {
                            CancelGuideIntroCard(
                                serviceName = uiState.serviceName,
                                logoUrl = uiState.logoUrl,
                            )
                        }

                        item {
                            if (uiState.hasCancelGuideUrl) {
                                SupportActionCard(
                                    title = "해지 가이드",
                                    value = uiState.cancelGuideUrlDisplayText ?: uiState.cancelGuideUrl.orEmpty(),
                                    description = "해지 절차는 서비스 정책에 따라 달라질 수 있어요.",
                                    actionText = "해지 가이드 열기",
                                    onActionClick = onOpenCancelGuideLink,
                                    actionIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                                )
                            } else {
                                AppCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                                        Text(
                                            text = "해지 가이드 링크가 아직 없어요.",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = "고객센터를 통해 직접 해지해 주세요.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.hasCustomerServicePhone) {
                            item {
                                SupportActionCard(
                                    title = "고객센터 전화번호",
                                    value = uiState.customerServicePhone.orEmpty(),
                                    description = "전화 앱으로 이동해 바로 문의할 수 있어요.",
                                    actionText = "고객센터 전화하기",
                                    onActionClick = onCallCustomerService,
                                    actionIcon = Icons.Outlined.Call,
                                )
                            }
                        }

                        if (uiState.hasContactEmail) {
                            item {
                                SupportActionCard(
                                    title = "문의 메일",
                                    value = uiState.contactEmail.orEmpty(),
                                    description = "메일 앱에서 문의 내용을 작성할 수 있어요.",
                                    actionText = "메일 보내기",
                                    onActionClick = onSendContactEmail,
                                    actionIcon = Icons.Outlined.Email,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CancelGuideIntroCard(
    serviceName: String,
    logoUrl: String?,
) {
    val spacing = LocalSpacing.current
    val serviceLabel = serviceName.ifBlank { "구독 서비스" }

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            ServiceBrandIcon(
                serviceName = serviceLabel,
                logoUrl = logoUrl,
                modifier = Modifier.size(56.dp),
                fallbackTextStyle = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = serviceLabel,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "해지하시려면 아래 안내를 확인해 주세요. 바로 연결이 어려운 경우에는 고객센터 연락 수단을 함께 확인할 수 있어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SupportActionCard(
    title: String,
    value: String,
    description: String,
    actionText: String,
    onActionClick: () -> Unit,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    val spacing = LocalSpacing.current

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppButton(
                text = actionText,
                onClick = onActionClick,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Medium,
                leadingIcon = actionIcon,
            )
        }
    }
}
