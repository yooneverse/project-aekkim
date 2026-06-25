package com.ssafy.e106.feature.auth.ui

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e106.core.ui.component.button.AppButton
import com.ssafy.e106.core.ui.component.card.AppCard
import com.ssafy.e106.core.ui.component.selection.AppAgreementRow
import com.ssafy.e106.core.ui.model.ButtonSize
import com.ssafy.e106.core.ui.model.ButtonVariant
import com.ssafy.e106.core.ui.theme.LocalSpacing
import com.ssafy.e106.feature.auth.AuthIntent
import com.ssafy.e106.feature.auth.AuthUiEffect
import com.ssafy.e106.feature.auth.AuthUiState
import com.ssafy.e106.feature.auth.AuthViewModel
import com.ssafy.e106.feature.auth.ConsentDetailLinks
import com.ssafy.e106.feature.auth.ConsentItem
import com.ssafy.e106.feature.auth.PermissionStep
import com.ssafy.e106.feature.auth.ui.component.AuthFlowScaffold
import com.ssafy.e106.feature.auth.ui.component.AuthHeroSection
import com.ssafy.e106.feature.auth.ui.component.AuthHeroVariant
import com.ssafy.e106.feature.auth.ui.component.PermissionPromptDialog
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TermsRoute(
    onNavigateToAnalysis: () -> Unit,
    onNavigateToServiceTerms: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToMarketingConsent: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val requestNotificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onIntent(
            AuthIntent.NotificationPermissionResult(
                granted = granted,
                usageStatsGranted = context.hasUsageStatsPermission(),
            ),
        )
    }
    val usageStatsSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.onIntent(AuthIntent.UsageStatsPermissionUpdated(context.hasUsageStatsPermission()))
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is AuthUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }

                AuthUiEffect.NavigateToAnalysis -> onNavigateToAnalysis()
                AuthUiEffect.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onIntent(
                            AuthIntent.NotificationPermissionResult(
                                granted = true,
                                usageStatsGranted = context.hasUsageStatsPermission(),
                            ),
                        )
                    }
                }

                AuthUiEffect.OpenUsageAccessSettings -> {
                    usageStatsSettingsLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }

                is AuthUiEffect.OpenConsentDetail -> {
                    when (effect.url) {
                        ConsentDetailLinks.SERVICE_TERMS -> onNavigateToServiceTerms()
                        ConsentDetailLinks.PRIVACY_POLICY -> onNavigateToPrivacyPolicy()
                        ConsentDetailLinks.MARKETING_CONSENT -> onNavigateToMarketingConsent()
                        else -> uriHandler.openUri(effect.url)
                    }
                }

                else -> Unit
            }
        }
    }

    TermsScreen(
        uiState = uiState,
        onToggleAll = { viewModel.onIntent(AuthIntent.AllConsentToggled) },
        onToggleItem = { itemId -> viewModel.onIntent(AuthIntent.ConsentToggled(itemId)) },
        onConsentDetailClick = { itemId -> viewModel.onIntent(AuthIntent.ConsentDetailClicked(itemId)) },
        onStartClick = { viewModel.onIntent(AuthIntent.StartClicked) },
        onNotificationPermissionConfirm = {
            viewModel.onIntent(AuthIntent.NotificationPermissionPromptConfirmed)
        },
        onNotificationPermissionSkip = {
            viewModel.onIntent(
                AuthIntent.NotificationPermissionResult(
                    granted = false,
                    usageStatsGranted = context.hasUsageStatsPermission(),
                ),
            )
        },
        onUsageStatsConfirm = { viewModel.onIntent(AuthIntent.UsageStatsAccessRequested) },
    )
}

@Composable
fun TermsScreen(
    uiState: AuthUiState,
    onToggleAll: () -> Unit,
    onToggleItem: (String) -> Unit,
    onConsentDetailClick: (String) -> Unit,
    onStartClick: () -> Unit,
    onNotificationPermissionConfirm: () -> Unit,
    onNotificationPermissionSkip: () -> Unit,
    onUsageStatsConfirm: () -> Unit,
) {
    val isSavingTerms = uiState.isTermsSaving
    val canStart = uiState.isAllRequiredConsented && !isSavingTerms
    val startButtonText = if (isSavingTerms) "저장 중..." else "동의하고 시작하기"
    val startButtonStateDescription = when {
        isSavingTerms -> "동의 내용을 저장 중"
        !uiState.isAllRequiredConsented -> "필수 동의가 필요함"
        else -> "시작 가능"
    }
    val spacing = LocalSpacing.current
    val requiredItems = uiState.consentItems.filter { it.required }
    val requiredAgreedCount = requiredItems.count { it.checked }
    val requiredProgress = if (requiredItems.isEmpty()) {
        1f
    } else {
        requiredAgreedCount.toFloat() / requiredItems.size.toFloat()
    }

    AuthFlowScaffold(
        scrollableBody = false,
        centerBody = true,
        bodyHorizontalAlignment = Alignment.CenterHorizontally,
        body = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
            ) {
                AuthHeroSection(
                    title = "흩어진 OTT 구독을\n한 번에 정리해요",
                    subtitle = "필수 동의만 마치면 바로 분석을 시작할 수 있어요",
                    variant = AuthHeroVariant.Screen,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    textAlign = TextAlign.Center,
                    topContent = { TermsHeroBranding() },
                )

                RequiredConsentProgressPanel(
                    requiredAgreedCount = requiredAgreedCount,
                    requiredTotalCount = requiredItems.size,
                    progress = requiredProgress,
                )

                TermsConsentSection(
                    consentItems = uiState.consentItems,
                    enabled = !uiState.isTermsSaving,
                    onToggleAll = onToggleAll,
                    onToggleItem = onToggleItem,
                    onConsentDetailClick = onConsentDetailClick,
                )

                Text(
                    text = "개인정보 처리방침에서\n자세한 내용을 확인할 수 있어요",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                PermissionStatusCard(permissionStep = uiState.permissionStep)
            }
        },
        bottomContent = {
            AppButton(
                text = startButtonText,
                onClick = onStartClick,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Primary,
                size = ButtonSize.Large,
                enabled = canStart,
                loading = false,
                accessibilityLabel = "시작하기 버튼",
                accessibilityStateDescription = startButtonStateDescription,
            )
        },
    )

    if (uiState.showNotificationPrimerDialog) {
        NotificationPermissionDialog(
            onConfirm = onNotificationPermissionConfirm,
            onSkip = onNotificationPermissionSkip,
        )
    }

    if (uiState.showUsageStatsDialog) {
        UsageStatsPermissionDialog(
            isRePrompt = uiState.showUsageStatsRePrompt,
            onConfirm = onUsageStatsConfirm,
        )
    }
}

@Composable
private fun TermsHeroBranding() {
    val spacing = LocalSpacing.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ) {
            Text(
                text = "필수 동의 확인",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun RequiredConsentProgressPanel(
    requiredAgreedCount: Int,
    requiredTotalCount: Int,
    progress: Float,
) {
    val spacing = LocalSpacing.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "requiredConsentProgress",
    )
    val percent = (animatedProgress * 100).toInt()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
                        ),
                    ),
                )
                .padding(horizontal = spacing.space4, vertical = spacing.space3),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Text(
                text = "필수 동의 $requiredAgreedCount / $requiredTotalCount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 8.dp)
                    .clip(RoundedCornerShape(percent = 50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                text = "$percent% 완료, 모두 동의하면 바로 시작할 수 있어요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TermsConsentSection(
    consentItems: List<ConsentItem>,
    enabled: Boolean,
    onToggleAll: () -> Unit,
    onToggleItem: (String) -> Unit,
    onConsentDetailClick: (String) -> Unit,
) {
    val spacing = LocalSpacing.current

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
        contentPadding = PaddingValues(
            horizontal = spacing.space3,
            vertical = spacing.space2,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
                        ),
                    ),
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            AppAgreementRow(
                label = "약관 모두 동의",
                checked = consentItems.all { it.checked },
                enabled = enabled,
                onClick = onToggleAll,
                emphasized = true,
                pressFeedbackEnabled = true,
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                thickness = 1.dp,
            )

            consentItems.forEachIndexed { index, item ->
                AppAgreementRow(
                    label = buildWrappedConsentLabel(required = item.required, label = item.label),
                    checked = item.checked,
                    enabled = enabled,
                    onClick = { onToggleItem(item.id) },
                    pressFeedbackEnabled = true,
                    trailingContent = {
                        Text(
                            text = "자세히",
                            modifier = Modifier
                                .clickable(enabled = enabled) {
                                    onConsentDetailClick(item.id)
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (enabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            },
                        )
                    },
                )

                if (index != consentItems.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        thickness = 1.dp,
                    )
                }
            }
        }
    }
}

private fun buildWrappedConsentLabel(required: Boolean, label: String): String {
    val prefix = if (required) "[필수]" else "[선택]"
    return wrapByWords(text = "$prefix $label", maxCharsPerLine = 16)
}

private fun wrapByWords(text: String, maxCharsPerLine: Int): String {
    val words = text.split(" ")
    if (words.isEmpty()) return text

    val builder = StringBuilder()
    var currentLineLength = 0
    words.forEachIndexed { index, word ->
        val additionalLength = if (currentLineLength == 0) word.length else word.length + 1
        val shouldWrap = currentLineLength > 0 && currentLineLength + additionalLength > maxCharsPerLine
        when {
            shouldWrap -> {
                builder.append("\n")
                builder.append(word)
                currentLineLength = word.length
            }

            currentLineLength == 0 -> {
                builder.append(word)
                currentLineLength = word.length
            }

            else -> {
                builder.append(" ")
                builder.append(word)
                currentLineLength += additionalLength
            }
        }

        if (index == words.lastIndex) return@forEachIndexed
    }
    return builder.toString()
}

@Composable
private fun PermissionStatusCard(permissionStep: PermissionStep) {
    val spacing = LocalSpacing.current
    if (permissionStep == PermissionStep.None) return

    val (badge, description) = when (permissionStep) {
        PermissionStep.Fcm -> "선택 권한 안내" to "알림 권한을 먼저 확인한 뒤, 이어서 앱 사용 기록 접근 권한을 안내해드릴게요."
        PermissionStep.UsageStats -> "필수 권한 안내" to "앱 사용 기록 접근 권한을 허용하면 자동 구독 분석을 바로 시작할 수 있어요."
        PermissionStep.None -> return
    }

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        contentPadding = PaddingValues(horizontal = spacing.space4, vertical = spacing.space4),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun NotificationPermissionDialog(
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
) {
    PermissionPromptDialog(
        badge = "선택 권한",
        title = "중요한 구독 일정만 깔끔하게\n알려드릴게요",
        description = "결제일, 체크인 시점, 혜택 업데이트처럼\n구독 관리에 필요한 알림만 보내요.",
        highlights = listOf(
            "체크인이 필요한 순간만 빠르게 리마인드해요.",
            "프로모션이나 혜택 변경도 필요한 내용만 선별해서\n알려드려요.",
        ),
        primaryActionLabel = "알림 허용하기",
        onPrimaryActionClick = onConfirm,
        secondaryActionLabel = "지금은 건너뛰기",
        onSecondaryActionClick = onSkip,
    )
}

@Composable
private fun UsageStatsPermissionDialog(
    isRePrompt: Boolean,
    onConfirm: () -> Unit,
) {
    val title = if (isRePrompt) {
        "자동 분석을 계속하려면 권한 확인이 필요해요"
    } else {
        "OTT 사용 기록 접근 권한이 필요해요"
    }
    val description = if (isRePrompt) {
        "설정에서 사용 기록 접근 권한을 허용하면, 지금 보던 흐름으로 바로 돌아와 분석을 이어갈 수 있어요."
    } else {
        "최근 사용 여부를 기기 안에서만 확인해서 정리할 구독 후보를 찾기 위해 필요해요."
    }
    val buttonLabel = if (isRePrompt) "다시 허용하러 가기" else "설정에서 허용하기"

    PermissionPromptDialog(
        badge = "필수 권한",
        title = title,
        description = description,
        highlights = listOf(
            "사용 기록 원문은 서버에 저장하지 않고, 기기 안에서만 분석해요.",
            "권한을 허용하면 분석 화면으로 자동 이동해서 다음 단계를 이어가요.",
        ),
        primaryActionLabel = buttonLabel,
        onPrimaryActionClick = onConfirm,
    )
}

private fun Context.hasUsageStatsPermission(): Boolean {
    val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOpsManager.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        packageName,
    )
    return mode == AppOpsManager.MODE_ALLOWED
}
