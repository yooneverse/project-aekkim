package com.ssafy.e106.feature.notification.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ssafy.e106.core.ui.component.card.AppCard
import com.ssafy.e106.core.ui.component.feedback.AppEmptyState
import com.ssafy.e106.core.ui.component.feedback.AppErrorState
import com.ssafy.e106.core.ui.component.feedback.AppLoadingState
import com.ssafy.e106.core.ui.theme.LocalSpacing
import com.ssafy.e106.data.dto.notification.NotificationType
import com.ssafy.e106.feature.notification.NotificationListItem
import com.ssafy.e106.feature.notification.NotificationUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    uiState: NotificationUiState,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onRetryLoad: () -> Unit,
    onNotificationClick: (Long) -> Unit,
    onNotificationDelete: (Long) -> Unit,
    onDeleteAllClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val interactionLocked = uiState.isClearingAll || uiState.deletingNotificationIds.isNotEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NotificationTopBar(
                onNavigateBack = onNavigateBack,
                onDeleteAllClick = onDeleteAllClick,
                canDeleteAll = uiState.notifications.isNotEmpty() && !interactionLocked,
                isClearingAll = uiState.isClearingAll,
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = {
                if (!interactionLocked) {
                    onRefresh()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    NotificationScreenStateContainer {
                        AppLoadingState(
                            title = "알림을 불러오는 중이에요",
                            description = "알림함을 새로고침하고 있어요",
                        )
                    }
                }

                uiState.error != null -> {
                    NotificationScreenStateContainer {
                        AppErrorState(
                            title = "알림을 불러오지 못했어요",
                            description = uiState.error,
                            onRetryClick = onRetryLoad,
                        )
                    }
                }

                uiState.isEmpty -> {
                    NotificationScreenStateContainer {
                        AppEmptyState(
                            title = "아직 받은 알림이 없어요",
                            description = "체크인, 프로모션, 해지 검토 관련 알림이 이곳에 표시돼요.",
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = spacing.screenHorizontalPadding,
                            end = spacing.screenHorizontalPadding,
                            top = spacing.space4,
                            bottom = spacing.space6,
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.space3),
                    ) {
                        items(
                            items = uiState.notifications,
                            key = { item -> item.notificationId },
                        ) { item ->
                            NotificationDismissibleCard(
                                item = item,
                                isReading = item.notificationId in uiState.readingNotificationIds,
                                enabled = !interactionLocked &&
                                    item.notificationId !in uiState.readingNotificationIds,
                                onClick = { onNotificationClick(item.notificationId) },
                                onDelete = { onNotificationDelete(item.notificationId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationTopBar(
    onNavigateBack: () -> Unit,
    onDeleteAllClick: () -> Unit,
    canDeleteAll: Boolean,
    isClearingAll: Boolean,
) {
    val actionModifier = if (canDeleteAll) {
        Modifier.clickable(onClick = onDeleteAllClick)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
            )
        }
        Text(
            text = "알림",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (isClearingAll) "지우는 중..." else "전체 지우기",
            modifier = actionModifier
                .clip(MaterialTheme.shapes.small)
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .alpha(
                    if (canDeleteAll || isClearingAll) {
                        1f
                    } else {
                        0.45f
                    },
                ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun NotificationScreenStateContainer(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationDismissibleCard(
    item: NotificationListItem,
    isReading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.35f },
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && enabled) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enabled,
        backgroundContent = {
            NotificationDeleteBackground()
        },
    ) {
        NotificationListCard(
            item = item,
            isReading = isReading,
            enabled = enabled,
            onClick = onClick,
        )
    }
}

@Composable
private fun NotificationDeleteBackground() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = "삭제",
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun NotificationListCard(
    item: NotificationListItem,
    isReading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    AppCard(onClick = if (enabled) onClick else null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(10.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (item.isRead) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    ),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NotificationTypeChip(type = item.type)
                    Text(
                        text = item.sentAt.asDisplayTimestamp(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.SemiBold,
                )
                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isReading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun NotificationTypeChip(
    type: NotificationType,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = type.displayLabel(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun NotificationType.displayLabel(): String =
    when (this) {
        NotificationType.CHECKIN -> "체크인"
        NotificationType.CHURN_REVIEW -> "해지 검토"
        NotificationType.PROMO -> "혜택"
    }

private fun String?.asDisplayTimestamp(): String {
    if (this.isNullOrBlank()) return "시간 정보 없음"
    return replace("T", " ").take(16)
}
