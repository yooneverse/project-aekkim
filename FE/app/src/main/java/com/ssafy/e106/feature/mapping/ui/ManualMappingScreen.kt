package com.ssafy.e106.feature.mapping.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssafy.e106.core.ui.component.button.AppButton
import com.ssafy.e106.core.ui.component.card.AppCard
import com.ssafy.e106.core.ui.component.feedback.AppEmptyState
import com.ssafy.e106.core.ui.component.feedback.AppErrorState
import com.ssafy.e106.core.ui.component.feedback.AppLoadingState
import com.ssafy.e106.core.ui.component.service.ServiceBrandIcon
import com.ssafy.e106.core.ui.model.ButtonSize
import com.ssafy.e106.core.ui.model.ButtonVariant
import com.ssafy.e106.core.ui.theme.LocalSpacing
import com.ssafy.e106.feature.dashboard.ManualAddStep
import com.ssafy.e106.feature.dashboard.ui.component.PlanBillingBottomSheet
import com.ssafy.e106.feature.dashboard.ui.component.ServiceSelectionBottomSheet
import com.ssafy.e106.feature.mapping.ManualMappingUiState
import com.ssafy.e106.feature.mapping.MerchantGroupCardItem
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ManualMappingScreen(
    uiState: ManualMappingUiState,
    onNavigateBack: () -> Unit,
    onRetryLoad: () -> Unit,
    onOpenAddFlow: (List<Long>) -> Unit,
    onDismissAddFlow: () -> Unit,
    onSelectService: (Long) -> Unit,
    onSelectPlan: (Long) -> Unit,
    onSelectBillingDay: (Int) -> Unit,
    onSubmitManualAdd: () -> Unit,
    onRemoveReview: (List<Long>) -> Unit,
    onToggleMerchantExpand: (String) -> Unit,
) {
    val spacing = LocalSpacing.current
    val pendingReviewGroupCount = uiState.merchantGroups.size

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = spacing.screenHorizontalPadding,
                    vertical = spacing.space6,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "back",
                    modifier = Modifier.clickable(onClick = onNavigateBack),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (pendingReviewGroupCount > 0)
                            "확인이 필요한 결제 묶음 ${pendingReviewGroupCount}건"
                        else
                            "확인이 필요한 결제 묶음",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "구독 여부가 애매한 결제 내역을 확인해주세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    AppLoadingState(
                        title = "확인이 필요한 결제를 불러오는 중입니다.",
                        description = "잠시만 기다려 주세요.",
                    )
                }

                uiState.error != null -> {
                    AppErrorState(
                        title = "결제 후보를 불러오지 못했습니다.",
                        description = uiState.error,
                        onRetryClick = onRetryLoad,
                    )
                }

                uiState.isEmpty -> {
                    AppEmptyState(
                        title = "확인이 필요한 결제가 없습니다.",
                        description = "새 검토 대상이 생기면 여기에서 서비스 연결을 진행할 수 있습니다.",
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing.space3),
                        contentPadding = PaddingValues(bottom = spacing.space1),
                    ) {
                        items(
                            items = uiState.merchantGroups,
                            key = { group -> group.merchantName },
                        ) { group ->
                            MerchantGroupCard(
                                group = group,
                                isExpanded = group.merchantName in uiState.expandedMerchants,
                                onToggleExpand = { onToggleMerchantExpand(group.merchantName) },
                                onAddService = { onOpenAddFlow(group.reviewIds) },
                                onRemove = { onRemoveReview(group.reviewIds) },
                            )
                        }
                    }
                }
            }
        }

        if (uiState.manualAddStep == ManualAddStep.SelectService) {
            ServiceSelectionBottomSheet(
                services = uiState.serviceOptions,
                onDismiss = onDismissAddFlow,
                onSelectService = onSelectService,
            )
        }

        if (
            uiState.manualAddStep == ManualAddStep.ConfigurePlanAndBillingDay &&
            uiState.selectedService != null
        ) {
            PlanBillingBottomSheet(
                selectedService = uiState.selectedService,
                planOptions = uiState.planOptions,
                selectedPlanId = uiState.selectedPlanId,
                selectedBillingDay = uiState.selectedBillingDay,
                isEditing = false,
                onDismiss = onDismissAddFlow,
                onSelectPlan = onSelectPlan,
                onSelectBillingDay = onSelectBillingDay,
                onSubmit = onSubmitManualAdd,
            )
        }
    }
}

@Composable
private fun MerchantGroupCard(
    group: MerchantGroupCardItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddService: () -> Unit,
    onRemove: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron_rotation",
    )

    AppCard(contentPadding = PaddingValues(0.dp)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = spacing.space4, vertical = spacing.space4),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ServiceBrandIcon(
                        serviceName = group.suggestedServiceName ?: group.merchantName,
                        logoUrl = null,
                        modifier = Modifier.size(44.dp),
                        fallbackLabel = (group.suggestedServiceName ?: group.merchantName)
                            .trim().firstOrNull()?.toString() ?: "?",
                        fallbackTextStyle = MaterialTheme.typography.titleMedium,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = group.suggestedServiceName ?: "서비스 선택 필요",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = group.merchantName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${group.transactions.size}건",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "접기" else "펼치기",
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(chevronRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    group.transactions.forEachIndexed { index, tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = spacing.space4, vertical = spacing.space3),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = tx.billedAtLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = formatWon(tx.monthlyAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (index < group.transactions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = spacing.space4),
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space4, vertical = spacing.space3),
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                AppButton(
                    text = "서비스 선택",
                    onClick = onAddService,
                    variant = ButtonVariant.Primary,
                    size = ButtonSize.Small,
                )
                AppButton(
                    text = "제거",
                    onClick = onRemove,
                    variant = ButtonVariant.Ghost,
                    size = ButtonSize.Small,
                )
            }
        }
    }
}

private fun formatWon(amount: Int): String {
    return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원"
}
