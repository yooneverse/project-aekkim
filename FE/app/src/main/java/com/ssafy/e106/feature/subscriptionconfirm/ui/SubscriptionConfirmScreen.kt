package com.ssafy.e106.feature.subscriptionconfirm.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.e106.R
import com.ssafy.e106.core.ui.component.button.AppButton
import com.ssafy.e106.core.ui.component.card.AppCard
import com.ssafy.e106.core.ui.component.feedback.AppEmptyState
import com.ssafy.e106.core.ui.component.feedback.AppErrorState
import com.ssafy.e106.core.ui.component.feedback.AppLoadingState
import com.ssafy.e106.core.ui.component.service.ServiceBrandIcon
import com.ssafy.e106.core.ui.model.ButtonSize
import com.ssafy.e106.core.ui.model.ButtonVariant
import com.ssafy.e106.core.ui.theme.LocalSpacing
import com.ssafy.e106.feature.subscriptionconfirm.DetectedSubscriptionItem
import com.ssafy.e106.feature.subscriptionconfirm.PendingReviewSummaryItem
import com.ssafy.e106.feature.subscriptionconfirm.SubscriptionConfirmUiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SubscriptionConfirmScreen(
    uiState: SubscriptionConfirmUiState,
    onRetryLoad: () -> Unit,
    onExcludeDetectedSubscription: (Long) -> Unit,
    onOpenPendingReview: () -> Unit,
    onConfirmDetectedSubscriptions: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val displayName = displayNickname(uiState.nickname)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!uiState.isLoading && uiState.error == null) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 2.dp,
                    shadowElevation = 6.dp,
                ) {
                    AppButton(
                        text = "확인완료",
                        onClick = onConfirmDetectedSubscriptions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = spacing.screenHorizontalPadding,
                                vertical = spacing.space4,
                            ),
                        loading = uiState.isCompletingLocalSessionOnly,
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    AppLoadingState(
                        title = "감지된 구독과 확인이 필요한 결제를 불러오는 중입니다.",
                        description = "잠시만 기다려 주세요.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = spacing.screenHorizontalPadding),
                    )
                }

                uiState.error != null -> {
                    AppErrorState(
                        title = "구독 확인 화면을 불러오지 못했습니다.",
                        description = uiState.error,
                        onRetryClick = onRetryLoad,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = spacing.screenHorizontalPadding),
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = spacing.screenHorizontalPadding,
                            end = spacing.screenHorizontalPadding,
                            top = spacing.space6,
                            bottom = spacing.space6,
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
                    ) {
                        item {
                            SubscriptionConfirmHeader(nickname = displayName)
                        }
                        item {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                        item {
                            SectionHeader(title = "내 구독 리스트")
                        }
                        if (uiState.isDetectedSubscriptionsEmpty) {
                            item {
                                AppEmptyState(
                                    title = "남아 있는 구독이 없어요.",
                                    description = if (uiState.pendingReviewSummary != null) {
                                        "확인이 필요한 결제는 아래에서 이어서 확인할 수 있어요."
                                    } else {
                                        "이번 분석에서 감지된 구독이 없어요. 확인완료를 누르면 대시보드로 이동해요."
                                    },
                                )
                            }
                        } else {
                            items(
                                items = uiState.visibleDetectedSubscriptions,
                                key = { item -> item.reviewId },
                            ) { item ->
                                DetectedSubscriptionCard(
                                    item = item,
                                    onExclude = { onExcludeDetectedSubscription(item.reviewId) },
                                )
                            }
                        }

                        uiState.pendingReviewSummary?.let { summary ->
                            item {
                                SectionHeader(title = "확인이 필요한 결제")
                            }
                            item {
                                PendingReviewSummaryCard(
                                    summary = summary,
                                    nickname = displayName,
                                    enabled = !uiState.isCompletingLocalSessionOnly,
                                    onOpenPendingReview = onOpenPendingReview,
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
private fun SubscriptionConfirmHeader(
    nickname: String,
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_aekkim),
            contentDescription = "Aekkim 로고",
            modifier = Modifier.height(44.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = "${nickname}님이 구독 중인\n서비스들입니다.",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "실제로 구독하고 있는 서비스인지 확인해주세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    description: String? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetectedSubscriptionCard(
    item: DetectedSubscriptionItem,
    onExclude: () -> Unit,
) {
    val spacing = LocalSpacing.current

    AppCard(contentPadding = PaddingValues(horizontal = spacing.space4, vertical = spacing.space4)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ServiceBrandIcon(
                serviceName = item.serviceName,
                logoUrl = item.logoUrl,
                modifier = Modifier.size(52.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.serviceName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatWon(item.monthlyAmount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "최근 결제일 ${item.billedAtLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppButton(
                text = "구독 아님",
                onClick = onExclude,
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Small,
            )
        }
    }
}

@Composable
private fun PendingReviewSummaryCard(
    summary: PendingReviewSummaryItem,
    nickname: String,
    enabled: Boolean,
    onOpenPendingReview: () -> Unit,
) {
    val spacing = LocalSpacing.current

    AppCard(
        contentPadding = PaddingValues(horizontal = spacing.space4, vertical = spacing.space4),
        onClick = if (enabled) onOpenPendingReview else null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "확인이 필요한 결제 ${summary.count}건",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "구독인지 확실하지 않아요. ${nickname}님의 확인이 필요해요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                summary.suggestedServiceNamesLabel?.let { label ->
                    Text(
                        text = "추천 서비스: $label",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun displayNickname(nickname: String): String = nickname.ifBlank { "회원" }

private fun formatWon(amount: Int): String {
    return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원"
}
