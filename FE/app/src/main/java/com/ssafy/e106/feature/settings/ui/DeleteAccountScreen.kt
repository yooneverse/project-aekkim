package com.ssafy.e106.feature.settings.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssafy.e106.core.ui.component.button.AppButton
import com.ssafy.e106.core.ui.component.card.AppCard
import com.ssafy.e106.core.ui.model.ButtonSize
import com.ssafy.e106.core.ui.model.ButtonVariant
import com.ssafy.e106.core.ui.theme.LocalSpacing
import com.ssafy.e106.feature.settings.DeleteAccountStep
import com.ssafy.e106.feature.settings.DeleteAccountUiState

@Composable
fun DeleteAccountScreen(
    uiState: DeleteAccountUiState,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onToggleFinalCheck: (Boolean) -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val stepProgress = when (uiState.step) {
        DeleteAccountStep.Warning -> 0.5f
        DeleteAccountStep.Confirm -> 1f
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = spacing.space2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onPreviousStep) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Text(
                        text = "계정 삭제",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                LinearProgressIndicator(
                    progress = { stepProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline,
                )
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val enter = slideInHorizontally { width -> if (forward) width else -width } + fadeIn()
                val exit = slideOutHorizontally { width -> if (forward) -width else width } + fadeOut()
                enter togetherWith exit
            },
            label = "delete_account_step",
        ) { step ->
            when (step) {
                DeleteAccountStep.Warning -> WarningStepContent(
                    modifier = Modifier.padding(innerPadding),
                    onNext = onNextStep,
                    onBack = onPreviousStep,
                )
                DeleteAccountStep.Confirm -> ConfirmStepContent(
                    modifier = Modifier.padding(innerPadding),
                    finalCheckAgreed = uiState.finalCheckAgreed,
                    isDeleting = uiState.isDeleting,
                    onToggleFinalCheck = onToggleFinalCheck,
                    onConfirmDelete = onConfirmDelete,
                )
            }
        }
    }
}

@Composable
private fun WarningStepContent(
    modifier: Modifier = Modifier,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.screenHorizontalPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.space2))

            Text(
                text = "계정을 삭제하시려고요?",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = "삭제하면 모든 데이터가 사라지고\n복구할 수 없어요.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(spacing.space6))

        AppButton(
            text = "삭제 진행",
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.Primary,
            size = ButtonSize.Large,
        )

        Spacer(modifier = Modifier.height(spacing.space3))

        AppButton(
            text = "돌아가기",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.Ghost,
            size = ButtonSize.Large,
        )

        Spacer(modifier = Modifier.height(spacing.space6))
    }
}

@Composable
private fun ConfirmStepContent(
    modifier: Modifier = Modifier,
    finalCheckAgreed: Boolean,
    isDeleting: Boolean,
    onToggleFinalCheck: (Boolean) -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.screenHorizontalPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(spacing.space7))

        Text(
            text = "마지막으로\n확인해 주세요",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(spacing.space3))

        Text(
            text = "삭제 후에는 되돌릴 수 없어요.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(spacing.space6))

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                Text(
                    text = "삭제되는 항목",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                BulletText("등록한 구독 서비스 및 요금제 정보")
                BulletText("월별 체크인 이력 및 이용 분석 데이터")
                BulletText("맞춤 프로모션 추천 및 절약 정보")
                BulletText("알림 설정 및 동의 내역")
                BulletText("연결된 Google 계정 연동 해제")
            }
        }

        Spacer(modifier = Modifier.height(spacing.space5))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                width = 1.dp,
                color = if (finalCheckAgreed) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            ),
        ) {
            Row(
                modifier = Modifier
                    .clickable(enabled = !isDeleting) {
                        onToggleFinalCheck(!finalCheckAgreed)
                    }
                    .padding(spacing.space4),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                Checkbox(
                    checked = finalCheckAgreed,
                    onCheckedChange = { checked -> onToggleFinalCheck(checked) },
                    enabled = !isDeleting,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
                Text(
                    text = "위 내용을 확인했으며, 계정 삭제에 동의합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(spacing.space6))

        AppButton(
            text = "계정 삭제",
            onClick = onConfirmDelete,
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.Primary,
            size = ButtonSize.Large,
            enabled = finalCheckAgreed && !isDeleting,
            loading = isDeleting,
        )

        Spacer(modifier = Modifier.height(spacing.space6))
    }
}

@Composable
private fun BulletText(text: String) {
    val spacing = LocalSpacing.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "\u2022",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
