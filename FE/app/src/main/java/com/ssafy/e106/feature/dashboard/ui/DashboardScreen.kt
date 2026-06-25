package com.ssafy.e106.feature.dashboard.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.e106.core.ui.component.button.AppButton
import com.ssafy.e106.core.ui.component.card.AppCard
import com.ssafy.e106.core.ui.component.feedback.AppLoadingState
import com.ssafy.e106.core.ui.component.image.ServiceLogoBadge
import com.ssafy.e106.core.ui.component.navigation.AppBottomTabBar
import com.ssafy.e106.core.ui.component.navigation.AppBottomTabItem
import com.ssafy.e106.core.ui.component.service.BundleBrandLogoCluster
import com.ssafy.e106.core.ui.component.service.BundleBrandVisualItem
import com.ssafy.e106.core.ui.model.ButtonSize
import com.ssafy.e106.core.ui.model.ButtonVariant
import com.ssafy.e106.core.ui.theme.AekkimThemeTokens
import com.ssafy.e106.core.ui.theme.LocalSpacing
import com.ssafy.e106.core.ui.theme.PillShape
import com.ssafy.e106.feature.dashboard.CheckinBannerState
import com.ssafy.e106.feature.dashboard.DashboardUiState
import com.ssafy.e106.feature.dashboard.SubscriptionCardItem
import com.ssafy.e106.feature.dashboard.ui.component.DashboardHeaderHamster
import com.ssafy.e106.feature.dashboard.ui.component.DashboardMascotMessageCard
import com.ssafy.e106.feature.dashboard.UsageReminderBannerState
import com.ssafy.e106.feature.dashboard.ui.component.ManualAddBottomSheet
import com.ssafy.e106.feature.dashboard.ui.component.SubscriptionDetailBottomSheet
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private data class DashboardMascotMessageUi(
    val message: String,
    val supportingText: String? = null,
    val onClick: (() -> Unit)? = null,
    val trailingIcon: ImageVector? = null,
    val trailingContentDescription: String? = null,
    val onTrailingIconClick: (() -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onRetryLoad: () -> Unit,
    onRetrySubscriptionDetail: () -> Unit,
    onDismissUsageReminderBanner: () -> Unit,
    onToggleEditMode: () -> Unit,
    onOpenAddFlow: () -> Unit,
    onDismissManualAddFlow: () -> Unit,
    onUpdateManualAddQuery: (String) -> Unit,
    onSelectService: (Long) -> Unit,
    onSelectBundle: (String) -> Unit,
    onSelectPlan: (Long) -> Unit,
    onSelectBillingDay: (Int) -> Unit,
    onSelectBillingDate: (LocalDate) -> Unit,
    onSubmitManualAdd: () -> Unit,
    onOpenSubscriptionDetail: (Long) -> Unit,
    onCloseSubscriptionDetail: () -> Unit,
    onStartEditSubscription: (Long) -> Unit,
    onDeleteSubscription: (Long) -> Unit,
    onOpenPromotionList: () -> Unit,
    onOpenInsight: () -> Unit,
    onOpenUsageReminderInsight: (Long) -> Unit,
    onOpenPromotionDetail: (Long) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenCheckIn: (Long) -> Unit,
    onOpenCancelGuide: (Long) -> Unit,
    onOpenManualMapping: () -> Unit,
    onOpenMyPage: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val mascotMessage = uiState.usageReminderBanner?.let { banner ->
        DashboardMascotMessageUi(
            message = banner.title,
            onClick = { onOpenUsageReminderInsight(banner.targetSubscriptionId) },
            onTrailingIconClick = onDismissUsageReminderBanner,
            trailingIcon = Icons.Outlined.Close,
            trailingContentDescription = "닫기",
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AppBottomTabBar(
                items = listOf(
                    AppBottomTabItem(
                        label = "구독",
                        icon = Icons.Outlined.Subscriptions,
                        selected = true,
                        onClick = {},
                    ),
                    AppBottomTabItem(
                        label = "인사이트",
                        icon = Icons.Outlined.Insights,
                        selected = false,
                        onClick = onOpenInsight,
                    ),
                    AppBottomTabItem(
                        label = "프로모션",
                        icon = Icons.Outlined.Campaign,
                        selected = false,
                        onClick = onOpenPromotionList,
                    ),
                    AppBottomTabItem(
                        label = "MY",
                        icon = Icons.Outlined.PersonOutline,
                        selected = false,
                        onClick = onOpenMyPage,
                    ),
                ),
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.screenHorizontalPadding, vertical = spacing.space6),
                ) {
                    AppLoadingState(
                        title = "대시보드 정보를 불러오는 중입니다.",
                        description = "구독 현황과 이번 달 결제 금액을 정리하고 있어요.",
                    )
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
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
                        DashboardHeader(
                            hasUnreadNotifications = uiState.hasUnreadNotifications,
                            onOpenNotifications = onOpenNotifications,
                            mascotMessage = mascotMessage,
                        )
                    }
                    item {
                        SummaryCard(
                            monthlyTotalAmount = uiState.monthlyTotalAmount,
                            monthlyExpectedSavingAmount = uiState.monthlyExpectedSavingAmount,
                            onOpenPromotionList = onOpenPromotionList,
                        )
                    }
                    uiState.usageReminderBanner?.takeIf {
                        mascotMessage == null
                    }?.let { banner ->
                        item {
                            UsageReminderBanner(
                                banner = banner,
                                onClick = { onOpenUsageReminderInsight(banner.targetSubscriptionId) },
                                onDismiss = onDismissUsageReminderBanner,
                            )
                        }
                    }
                    if (uiState.isStale) {
                        item {
                            Text(
                                text = "최신 정보 동기화가 지연되고 있어요. 잠시 후 다시 확인해 주세요.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    uiState.pendingCheckinBanner?.let { banner ->
                        item {
                            CheckinBanner(
                                banner = banner,
                                onClick = { onOpenCheckIn(banner.subscriptionId) },
                            )
                        }
                    }
                    if (uiState.pendingReviewCount > 0) {
                        item {
                            PendingReviewBanner(
                                count = uiState.pendingReviewCount,
                                onClick = onOpenManualMapping,
                            )
                        }
                    }
                    item {
                        SubscriptionSectionCard(
                            subscriptions = uiState.subscriptions,
                            isEditMode = uiState.isEditMode,
                            pendingDeletedSubscriptionIds = uiState.pendingDeletedSubscriptionIds,
                            isApplyingEditChanges = uiState.isApplyingEditChanges,
                            isEmpty = uiState.isEmpty,
                            hasError = uiState.error != null,
                            onOpenAddFlow = onOpenAddFlow,
                            onToggleEditMode = onToggleEditMode,
                            onRetryLoad = onRetryLoad,
                            onOpenSubscriptionDetail = onOpenSubscriptionDetail,
                            onDeleteSubscription = onDeleteSubscription,
                        )
                    }
                }
            }
        }

        if (uiState.manualAddStep != null) {
            ManualAddBottomSheet(
                manualAddStep = uiState.manualAddStep,
                searchQuery = uiState.manualAddQuery,
                services = uiState.serviceOptions,
                serviceSearchResults = uiState.serviceSearchResults,
                bundleSearchResults = uiState.bundleSearchResults,
                selectedService = uiState.selectedService,
                selectedBundle = uiState.selectedBundle,
                planOptions = uiState.planOptions,
                selectedPlanId = uiState.selectedPlanId,
                selectedBillingDay = uiState.selectedBillingDay,
                selectedBillingDate = uiState.selectedBillingDate,
                isEditing = uiState.editingSubscriptionId != null,
                onDismiss = onDismissManualAddFlow,
                onSearchQueryChange = onUpdateManualAddQuery,
                onSelectService = onSelectService,
                onSelectBundle = onSelectBundle,
                onSelectPlan = onSelectPlan,
                onSelectBillingDay = onSelectBillingDay,
                onSelectBillingDate = onSelectBillingDate,
                onSubmit = onSubmitManualAdd,
            )
        }

        if (uiState.selectedSubscriptionId != null) {
            SubscriptionDetailBottomSheet(
                detail = uiState.subscriptionDetail,
                isLoading = uiState.isDetailLoading,
                errorMessage = uiState.subscriptionDetailError,
                onRetryLoad = onRetrySubscriptionDetail,
                onDismiss = onCloseSubscriptionDetail,
                onOpenPromotionDetail = onOpenPromotionDetail,
                onOpenCancelGuide = onOpenCancelGuide,
                onEditSubscription = onStartEditSubscription,
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    hasUnreadNotifications: Boolean,
    onOpenNotifications: () -> Unit,
    mascotMessage: DashboardMascotMessageUi?,
) {
    if (mascotMessage == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
        ) {
            DashboardHeaderHamster(
                hasUnreadNotifications = hasUnreadNotifications,
                modifier = Modifier.fillMaxWidth(),
            )
            DashboardNotificationButton(
                hasUnreadNotifications = hasUnreadNotifications,
                onOpenNotifications = onOpenNotifications,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DashboardMascotMessageCard(
                message = mascotMessage.message,
                supportingText = mascotMessage.supportingText,
                onClick = mascotMessage.onClick,
                trailingIcon = mascotMessage.trailingIcon,
                trailingContentDescription = mascotMessage.trailingContentDescription,
                onTrailingIconClick = mascotMessage.onTrailingIconClick,
                modifier = Modifier.weight(1f),
            )
            DashboardNotificationButton(
                hasUnreadNotifications = hasUnreadNotifications,
                onOpenNotifications = onOpenNotifications,
            )
        }
    }
}

@Composable
private fun DashboardNotificationButton(
    hasUnreadNotifications: Boolean,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clickable(onClick = onOpenNotifications),
    ) {
        Icon(
            imageVector = Icons.Outlined.NotificationsNone,
            contentDescription = "알림",
            modifier = Modifier.align(Alignment.Center),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        if (hasUnreadNotifications) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 1.dp, y = (-1).dp)
                    .size(10.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.background,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun SummaryCard(
    monthlyTotalAmount: Int,
    monthlyExpectedSavingAmount: Int,
    onOpenPromotionList: () -> Unit,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "이번 달 구독료",
                style = MaterialTheme.typography.bodyMedium,
                color = AekkimThemeTokens.colors.textMuted,
            )
            Text(
                text = formatWon(monthlyTotalAmount),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (monthlyExpectedSavingAmount > 0) {
                Text(
                    text = "최대 ${formatWon(monthlyExpectedSavingAmount)}을 아낄 수 있어요",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenPromotionList),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun UsageReminderBanner(
    banner: UsageReminderBannerState,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    BannerSurface(
        onClick = onClick,
        containerColor = lerp(
            start = MaterialTheme.colorScheme.surface,
            stop = MaterialTheme.colorScheme.primary,
            fraction = 0.08f,
        ),
        borderColor = lerp(
            start = MaterialTheme.colorScheme.outline,
            stop = MaterialTheme.colorScheme.primary,
            fraction = 0.28f,
        ),
    ) {
        Icon(
            imageVector = Icons.Outlined.NotificationsNone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = banner.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "닫기",
                tint = AekkimThemeTokens.colors.textMuted,
            )
        }
    }
}

@Composable
private fun CheckinBanner(
    banner: CheckinBannerState,
    onClick: () -> Unit,
) {
    BannerSurface(onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.NotificationsNone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = banner.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = banner.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = AekkimThemeTokens.colors.textMuted,
        )
    }
}

@Composable
private fun PendingReviewBanner(
    count: Int,
    onClick: () -> Unit,
) {
    val containerColor = lerp(
        start = MaterialTheme.colorScheme.surface,
        stop = MaterialTheme.colorScheme.primary,
        fraction = 0.12f,
    )
    val borderColor = lerp(
        start = MaterialTheme.colorScheme.outline,
        stop = MaterialTheme.colorScheme.primary,
        fraction = 0.35f,
    )

    BannerSurface(
        onClick = onClick,
        containerColor = containerColor,
        borderColor = borderColor,
    ) {
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "검토가 필요한 결제 내역 ${count}건",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = AekkimThemeTokens.colors.textMuted,
        )
    }
}

@Composable
private fun BannerSurface(
    onClick: (() -> Unit)?,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun SubscriptionSectionCard(
    subscriptions: List<SubscriptionCardItem>,
    isEditMode: Boolean,
    pendingDeletedSubscriptionIds: Set<Long>,
    isApplyingEditChanges: Boolean,
    isEmpty: Boolean,
    hasError: Boolean,
    onOpenAddFlow: () -> Unit,
    onToggleEditMode: () -> Unit,
    onRetryLoad: () -> Unit,
    onOpenSubscriptionDetail: (Long) -> Unit,
    onDeleteSubscription: (Long) -> Unit,
) {
    val spacing = LocalSpacing.current

    AppCard(contentPadding = PaddingValues(spacing.cardPadding)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
        ) {
            SubscriptionSectionHeader(
                isEditMode = isEditMode,
                isApplyingEditChanges = isApplyingEditChanges,
                onOpenAddFlow = onOpenAddFlow,
                onToggleEditMode = onToggleEditMode,
            )

            when {
                hasError -> {
                    SubscriptionSectionStatus(
                        title = "구독 리스트를 불러오지 못했어요",
                        description = "잠시 후 다시 시도해 주세요.",
                        icon = Icons.Outlined.CloudOff,
                        actionText = "다시 시도",
                        actionVariant = ButtonVariant.Secondary,
                        onActionClick = onRetryLoad,
                    )
                }

                isEmpty -> {
                    SubscriptionSectionStatus(
                        title = "등록된 구독이 없어요",
                        description = "추가 버튼으로 구독을 등록해 보세요.",
                        icon = Icons.Outlined.Inbox,
                        actionText = "구독 추가",
                        onActionClick = onOpenAddFlow,
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(spacing.space3),
                    ) {
                        subscriptions.forEach { item ->
                            SubscriptionRowCard(
                                item = item,
                                isEditMode = isEditMode,
                                isPendingDelete = item.subscriptionId in pendingDeletedSubscriptionIds,
                                isApplyingEditChanges = isApplyingEditChanges,
                                onClick = { onOpenSubscriptionDetail(item.subscriptionId) },
                                onDelete = { onDeleteSubscription(item.subscriptionId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionSectionHeader(
    isEditMode: Boolean,
    isApplyingEditChanges: Boolean,
    onOpenAddFlow: () -> Unit,
    onToggleEditMode: () -> Unit,
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = "내 구독 리스트",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isEditMode) {
                HeaderAction(text = "추가", icon = Icons.Outlined.Add, onClick = onOpenAddFlow)
            }
            HeaderAction(
                text = if (isEditMode) "완료" else "편집",
                icon = if (isEditMode) Icons.Outlined.Done else Icons.Outlined.Edit,
                onClick = if (isApplyingEditChanges) ({}) else onToggleEditMode,
            )
        }
    }
}

@Composable
private fun HeaderAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = Modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.space1),
        horizontalArrangement = Arrangement.spacedBy(spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = AekkimThemeTokens.colors.textSubtle,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = AekkimThemeTokens.colors.textSubtle,
        )
    }
}

@Composable
private fun SubscriptionSectionStatus(
    title: String,
    description: String,
    icon: ImageVector,
    actionText: String? = null,
    actionVariant: ButtonVariant = ButtonVariant.Primary,
    onActionClick: (() -> Unit)? = null,
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AekkimThemeTokens.colors.textMuted,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        if (actionText != null && onActionClick != null) {
            AppButton(
                text = actionText,
                onClick = onActionClick,
                variant = actionVariant,
                size = ButtonSize.Medium,
            )
        }
    }
}

@Composable
private fun SubscriptionRowCard(
    item: SubscriptionCardItem,
    isEditMode: Boolean,
    isPendingDelete: Boolean,
    isApplyingEditChanges: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val billingDateText = item.billingDateLabel()
    val rowModifier = if (isEditMode) {
        Modifier
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Column(
        modifier = rowModifier
            .fillMaxWidth()
            .alpha(if (isPendingDelete) 0.52f else 1f)
            .padding(vertical = spacing.space2),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SubscriptionBrandLeading(item = item)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.serviceName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (item.isYearlyBilling()) {
                        BillingCycleBadge(text = "연간")
                    }
                }
                Text(
                    text = billingDateText ?: item.missingBillingDateLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (billingDateText != null) {
                        AekkimThemeTokens.colors.textMuted
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                item.savingLabel?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (isPendingDelete) {
                    Text(
                        text = "제거 예정",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (isEditMode) {
                AppButton(
                    text = if (isPendingDelete) "취소" else "제거",
                    onClick = if (isApplyingEditChanges) ({}) else onDelete,
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Small,
                )
            } else {
                Column(
                    modifier = Modifier.padding(start = spacing.space2),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(spacing.space1),
                ) {
                    Text(
                        text = item.priceLabel(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                    )
                    item.nudgeMessage?.takeIf { message -> message.isNotBlank() }?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionBrandLeading(item: SubscriptionCardItem) {
    val bundleVisuals = item.toBundleBrandVisuals()
    val bundleBorder = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
    )

    Box(
        modifier = Modifier.width(68.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (item.subscriptionType.equals("BUNDLE", ignoreCase = true) && bundleVisuals.size > 1) {
            BundleBrandLogoCluster(
                services = bundleVisuals,
                logoSize = 28.dp,
                overlap = 8.dp,
                maxVisibleLogos = 3,
                showOverflowCount = true,
                logoContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                logoBorder = bundleBorder,
            )
        } else {
            ServiceLogoBadge(
                serviceName = item.serviceName,
                logoUrl = item.logoUrl,
                modifier = Modifier.size(52.dp),
            )
        }
    }
}

private fun SubscriptionCardItem.toBundleBrandVisuals(): List<BundleBrandVisualItem> {
    return coveredServices
        .distinctBy { service -> service.code.ifBlank { service.name } }
        .map { service ->
            BundleBrandVisualItem(
                name = service.name,
                code = service.code,
                logoUrl = service.logoUrl,
            )
        }
}

@Composable
private fun BillingCycleBadge(text: String) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun SubscriptionCardItem.isYearlyBilling(): Boolean = billingCycle.equals("YEARLY", ignoreCase = true)

private fun SubscriptionCardItem.billingDateLabel(): String? {
    val parsedBillingDate = nextBillingDate
        ?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }

    return if (isYearlyBilling()) {
        parsedBillingDate?.let { date -> "매년 ${date.monthValue}월 ${date.dayOfMonth}일 결제" }
    } else {
        parsedBillingDate?.let { date -> "매월 ${date.dayOfMonth}일 결제" }
            ?: nextBillingDate?.takeLast(2)?.toIntOrNull()?.let { day -> "매월 ${day}일 결제" }
    }
}

private fun SubscriptionCardItem.missingBillingDateLabel(): String {
    return if (isYearlyBilling()) "연간 결제일 정보를 확인해 주세요" else "결제일 정보를 확인해 주세요"
}

private fun SubscriptionCardItem.priceLabel(): String {
    val formattedPrice = formatWon(monthlyPrice)
    return if (isYearlyBilling()) "월 환산 $formattedPrice" else formattedPrice
}

private fun formatWon(amount: Int): String {
    return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원"
}
