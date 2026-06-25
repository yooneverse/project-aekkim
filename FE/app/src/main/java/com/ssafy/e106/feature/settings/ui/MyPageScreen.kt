package com.ssafy.e106.feature.settings.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.ssafy.e106.core.ui.component.button.AppButton
import com.ssafy.e106.core.ui.component.card.AppCard
import com.ssafy.e106.core.ui.component.feedback.AppErrorState
import com.ssafy.e106.core.ui.component.feedback.AppLoadingState
import com.ssafy.e106.core.ui.component.navigation.AppBottomTabBar
import com.ssafy.e106.core.ui.component.navigation.AppBottomTabItem
import com.ssafy.e106.core.ui.model.ButtonSize
import com.ssafy.e106.core.ui.model.ButtonVariant
import com.ssafy.e106.core.ui.theme.Disabled
import com.ssafy.e106.core.ui.theme.LocalSpacing
import com.ssafy.e106.feature.settings.MyPageUiState

@Composable
fun MyPageScreen(
    uiState: MyPageUiState,
    onRetryLoad: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToInsight: () -> Unit,
    onNavigateToPromotionList: () -> Unit,
    onCheckinAlertToggle: (Boolean) -> Unit,
    onPromoAlertToggle: (Boolean) -> Unit,
    onOptionalConsentToggle: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onMarketingConsentClick: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onLogoutClick: () -> Unit,
    onDismissLogoutDialog: () -> Unit,
    onConfirmLogout: () -> Unit,
    onDeleteAccountClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val showInitialLoading = uiState.isLoading && uiState.displayName.isBlank() && uiState.error == null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AppBottomTabBar(
                items = listOf(
                    AppBottomTabItem(
                        label = "구독",
                        icon = Icons.Outlined.Subscriptions,
                        selected = false,
                        onClick = onNavigateToDashboard,
                    ),
                    AppBottomTabItem(
                        label = "인사이트",
                        icon = Icons.Outlined.Insights,
                        selected = false,
                        onClick = onNavigateToInsight,
                    ),
                    AppBottomTabItem(
                        label = "추천",
                        icon = Icons.Outlined.Campaign,
                        selected = false,
                        onClick = onNavigateToPromotionList,
                    ),
                    AppBottomTabItem(
                        label = "MY",
                        icon = Icons.Outlined.PersonOutline,
                        selected = true,
                        onClick = {},
                    ),
                ),
            )
        },
    ) { innerPadding ->
        if (showInitialLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .padding(
                        horizontal = spacing.screenHorizontalPadding,
                        vertical = spacing.space6,
                    ),
            ) {
                AppLoadingState(
                    title = "마이페이지를 불러오는 중이에요.",
                    description = "잠시만 기다려주세요.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = spacing.screenHorizontalPadding,
                    end = spacing.screenHorizontalPadding,
                    top = spacing.space5,
                    bottom = spacing.space6,
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
            ) {
                uiState.error?.let { error ->
                    item {
                        AppErrorState(
                            title = "마이페이지를 불러오지 못했어요.",
                            description = error,
                            retryText = "다시 시도",
                            onRetryClick = onRetryLoad,
                        )
                    }
                }

                item {
                    ProfileCard(
                        profileImageUrl = uiState.profileImageUrl,
                        displayName = uiState.displayName.ifBlank { "사용자" },
                        email = uiState.email,
                        linkedProviderLabel = uiState.linkedProviderLabel,
                    )
                }

                item {
                    SectionBlock(title = "알림 설정") {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                            CardList {
                                ListRow(
                                    title = "시스템 알림 권한",
                                    trailingText = if (uiState.appNotificationsEnabled) "켜짐" else "꺼짐",
                                    onClick = onOpenNotificationSettings,
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                                SettingToggleRow(
                                    title = "체크인 알림 받기",
                                    checked = uiState.checkinAlertEnabled,
                                    onCheckedChange = onCheckinAlertToggle,
                                    enabled = !uiState.isSavingAlerts,
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                                SettingToggleRow(
                                    title = "혜택 알림 받기",
                                    checked = uiState.promoAlertEnabled,
                                    onCheckedChange = onPromoAlertToggle,
                                    enabled = !uiState.isSavingAlerts,
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                                SettingToggleRow(
                                    title = "혜택 정보 수신 동의",
                                    checked = uiState.optionalConsentAgreed,
                                    onCheckedChange = onOptionalConsentToggle,
                                    enabled = !uiState.isSavingConsent,
                                )
                            }
                            Text(
                                text = if (uiState.appNotificationsEnabled) {
                                    "아래 토글은 앱 내 알림 수신 설정입니다."
                                } else {
                                    "시스템 알림 권한이 꺼져 있어 실제 알림이 보이지 않을 수 있어요."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item {
                    SectionBlock(title = "앱 정보") {
                        CardList {
                            ListRow(
                                title = "앱 버전",
                                trailingText = uiState.appVersion,
                                showChevron = false,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                            ListRow(
                                title = "서비스 이용약관",
                                onClick = onTermsClick,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                            ListRow(
                                title = "개인정보 처리방침",
                                onClick = onPrivacyPolicyClick,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                            ListRow(
                                title = "혜택 정보 수신 동의",
                                onClick = onMarketingConsentClick,
                            )
                        }
                    }
                }

                item {
                    SectionBlock(title = "계정") {
                        AppCard(
                            contentPadding = PaddingValues(0.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            ListRow(
                                title = "로그아웃",
                                onClick = onLogoutClick,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                            ListRow(
                                title = "회원 탈퇴",
                                onClick = onDeleteAccountClick,
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showLogoutDialog) {
        SettingsConfirmationDialog(
            title = "로그아웃",
            message = "${uiState.linkedProviderLabel} 계정에서 로그아웃할까요?",
            confirmText = "로그아웃",
            isLoading = uiState.isLoggingOut,
            onDismiss = onDismissLogoutDialog,
            onConfirm = onConfirmLogout,
        )
    }
}

@Composable
private fun SectionBlock(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun ProfileCard(
    profileImageUrl: String?,
    displayName: String,
    email: String?,
    linkedProviderLabel: String,
) {
    val spacing = LocalSpacing.current
    val avatarSize = spacing.space8 + spacing.space6

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(
                profileImageUrl = profileImageUrl,
                displayName = displayName,
                size = avatarSize,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.space1),
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                email?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "$linkedProviderLabel 계정 연결",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    profileImageUrl: String?,
    displayName: String,
    size: Dp,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        if (profileImageUrl.isNullOrBlank()) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = displayName.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = "$displayName 프로필 이미지",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun CardList(
    content: @Composable ColumnScope.() -> Unit,
) {
    AppCard(contentPadding = PaddingValues(0.dp), content = content)
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val spacing = LocalSpacing.current
    val rowMinHeight = spacing.space8 + spacing.space3

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = rowMinHeight)
            .then(
                if (enabled) {
                    Modifier.clickable { onCheckedChange(!checked) }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = spacing.cardPadding, vertical = spacing.space3),
        horizontalArrangement = Arrangement.spacedBy(spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        )
        PillToggle(checked = checked, enabled = enabled)
    }
}

@Composable
private fun PillToggle(
    checked: Boolean,
    enabled: Boolean = true,
) {
    val trackWidth = 64.dp
    val thumbSize = 24.dp
    val thumbPadding = 3.dp

    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else Disabled,
        animationSpec = tween(durationMillis = 300),
        label = "trackBg",
    )
    val thumbOffsetX by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - thumbPadding else thumbPadding,
        animationSpec = tween(durationMillis = 300),
        label = "thumbX",
    )
    val onTextAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "onAlpha",
    )
    val offTextAlpha by animateFloatAsState(
        targetValue = if (checked) 0f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "offAlpha",
    )
    val trackElevation by animateDpAsState(
        targetValue = if (checked) 4.dp else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "trackElevation",
    )

    Surface(
        modifier = Modifier
            .size(width = trackWidth, height = 30.dp)
            .alpha(if (enabled) 1f else 0.38f),
        shape = RoundedCornerShape(percent = 50),
        color = trackColor,
        shadowElevation = trackElevation,
    ) {
        Box {
            Text(
                text = "ON",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .alpha(onTextAlpha),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "OFF",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 7.dp)
                    .alpha(offTextAlpha),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbOffsetX)
                    .size(thumbSize)
                    .shadow(2.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
            )
        }
    }
}

@Composable
private fun ListRow(
    title: String,
    titleColor: Color = Color.Unspecified,
    trailingText: String? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val spacing = LocalSpacing.current
    val rowMinHeight = spacing.space8 + spacing.space3

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = rowMinHeight)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = spacing.cardPadding, vertical = spacing.space3),
        horizontalArrangement = Arrangement.spacedBy(spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (titleColor != Color.Unspecified) titleColor else MaterialTheme.colorScheme.onSurface,
        )
        trailingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showChevron && onClick != null) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val spacing = LocalSpacing.current

    Dialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.space6),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = spacing.space5, vertical = spacing.space5),
                verticalArrangement = Arrangement.spacedBy(spacing.space4),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppButton(
                        text = "취소",
                        onClick = onDismiss,
                        variant = ButtonVariant.Secondary,
                        size = ButtonSize.Medium,
                        enabled = !isLoading,
                    )
                    AppButton(
                        text = confirmText,
                        onClick = onConfirm,
                        variant = ButtonVariant.Primary,
                        size = ButtonSize.Medium,
                        loading = isLoading,
                        enabled = !isLoading,
                    )
                }
            }
        }
    }
}
