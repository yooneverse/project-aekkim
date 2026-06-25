package com.ssafy.e106.feature.analysis.view

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.ssafy.e106.core.ui.component.button.AppButton
import com.ssafy.e106.core.ui.component.card.AppCard
import com.ssafy.e106.core.ui.model.ButtonSize
import com.ssafy.e106.core.ui.model.ButtonVariant
import com.ssafy.e106.core.ui.theme.AekkimThemeTokens
import com.ssafy.e106.core.ui.theme.LocalSpacing
import com.ssafy.e106.core.ui.theme.PillShape
import com.ssafy.e106.feature.analysis.model.AnalysisIntent
import com.ssafy.e106.feature.analysis.model.AnalysisStep
import com.ssafy.e106.feature.analysis.model.AnalysisUiEffect
import com.ssafy.e106.feature.analysis.model.AnalysisUiState
import com.ssafy.e106.feature.analysis.model.UsagePermissionState
import com.ssafy.e106.feature.analysis.viewmodel.AnalysisViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private const val DETECTIVE_HAMSTER_ASSET = "sherlock_hamzzi.png"
private val HeroDescriptionMinHeight = 52.dp
private val HeroClueChipMinHeight = 48.dp

@Composable
fun AnalysisLoadingRoute(
    onNavigateToSubscriptionConfirm: () -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    BackHandler(enabled = true) {
        // Keep the onboarding flow from returning to the terms screen mid-analysis.
    }

    LaunchedEffect(viewModel) {
        viewModel.onIntent(AnalysisIntent.Entered)
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                AnalysisUiEffect.NavigateToSubscriptionConfirm -> onNavigateToSubscriptionConfirm()
                AnalysisUiEffect.OpenUsageAccessSettings -> {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "설정 화면을 열 수 없어요.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onIntent(AnalysisIntent.RecheckPermissionOnResume)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AnalysisLoadingScreen(
        uiState = uiState,
        onOpenSettingsClick = {
            viewModel.onIntent(AnalysisIntent.OpenSettingsClicked)
        },
        onContinueClick = {
            viewModel.onIntent(AnalysisIntent.ContinueClicked)
        },
    )
}

@Composable
fun AnalysisLoadingScreen(
    uiState: AnalysisUiState,
    onOpenSettingsClick: () -> Unit,
    onContinueClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (uiState.permissionState) {
            UsagePermissionState.UNKNOWN -> AnalysisCheckingState()
            UsagePermissionState.DENIED -> PermissionRequiredState(onOpenSettingsClick = onOpenSettingsClick)
            UsagePermissionState.GRANTED -> AnalysisProgressState(
                uiState = uiState,
                onContinueClick = onContinueClick,
            )
        }
    }
}

@Composable
private fun AnalysisCheckingState() {
    AnalysisScreenColumn {
        DetectiveHamsterHeroCard(
            message = "탐정 햄스터가 출동할 준비를 하고 있어요",
            clueLabels = defaultDetectiveClues(),
            modifier = Modifier.fillMaxWidth(),
        )
        SectionIntro(
            eyebrow = "권한 확인",
            title = "사용 기록 권한을 확인하고 있어요",
            description = "권한 확인이 끝나면 흩어진 구독 흔적을 바로 정리할게요.",
        )
    }
}

@Composable
private fun PermissionRequiredState(
    onOpenSettingsClick: () -> Unit,
) {
    val spacing = LocalSpacing.current

    AnalysisScreenColumn {
        DetectiveHamsterHeroCard(
            message = "권한만 열어주면 바로 구독 흔적을 찾을게요",
            clueLabels = defaultDetectiveClues(),
            modifier = Modifier.fillMaxWidth(),
        )
        SectionIntro(
            eyebrow = "권한 필요",
            title = "구독을 찾으려면\n사용 기록 권한이 필요해요",
            description = "설정에서 사용 기록 접근을 허용하면 이 화면으로 돌아왔을 때 자동으로 다시 확인해요.",
        )

        AppCard(
            contentPadding = PaddingValues(
                horizontal = spacing.space4,
                vertical = spacing.space4,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "왜 필요한가요?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "최근 사용 흔적과 결제 기록을 함께 살펴봐야 숨어 있는 구독 가능성을 더 정확하게 찾을 수 있어요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        AppButton(
            text = "설정으로 이동",
            onClick = onOpenSettingsClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AnalysisProgressState(
    uiState: AnalysisUiState,
    onContinueClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val rawProgress = progressFraction(uiState)
    val progress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 500),
        label = "analysisProgress",
    )
    val hasUsageHistory = uiState.usageItems.any { it.lastUsedEpochMs != null || it.usage30dMs > 0L }
    val rotatingMessage = rememberRotatingMessage(
        currentStep = uiState.currentStep,
        isCollecting = uiState.isCollecting,
    )

    AnalysisScreenColumn(sectionGap = spacing.space4) {
        AnalysisHeadline(
            title = "흩어진 구독 흔적을\n찾아 정리하고 있어요",
        )

        DetectiveHamsterHeroCard(
            message = rotatingMessage,
            clueLabels = detectiveCluesForStep(uiState.currentStep),
            modifier = Modifier.fillMaxWidth(),
        )

        SavingsAmountCard(
            amount = uiState.savingAmount,
            hasUsageHistory = hasUsageHistory,
            isCollecting = uiState.isCollecting,
            showAmount = !uiState.isCollecting || (rawProgress >= 0.5f && hasUsageHistory),
        )

        AnalysisProgressStrip(
            progress = progress,
            currentStep = uiState.currentStep,
            isCollecting = uiState.isCollecting,
        )

        if (!uiState.isCollecting) {
            AppButton(
                text = "바로 확인하기",
                onClick = onContinueClick,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Medium,
            )
        }
    }
}

@Composable
private fun AnalysisScreenColumn(
    sectionGap: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalSpacing.current
    val resolvedGap = sectionGap ?: spacing.sectionGap

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = spacing.screenHorizontalPadding,
                vertical = spacing.space6,
            ),
        verticalArrangement = Arrangement.spacedBy(resolvedGap),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun AnalysisHeadline(
    title: String,
    description: String? = null,
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SectionIntro(
    eyebrow: String,
    title: String,
    description: String,
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        Surface(
            shape = PillShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = eyebrow,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = AekkimThemeTokens.colors.textMuted,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DetectiveHamsterHeroCard(
    message: String,
    clueLabels: List<String>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val transition = rememberInfiniteTransition(label = "detectiveHamsterHero")
    val patrolPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "heroPatrol",
    )
    val pulsePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heroPulse",
    )
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data("file:///android_asset/$DETECTIVE_HAMSTER_ASSET")
            .crossfade(true)
            .build(),
    )
    val normalizedPatrol = if (patrolPhase <= 1f) patrolPhase else 2f - patrolPhase
    val isMovingRight = patrolPhase <= 1f

    AppCard(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = spacing.space4,
            vertical = spacing.space4,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = PillShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = message,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineBreak = LineBreak.Paragraph,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(264.dp),
                contentAlignment = Alignment.Center,
            ) {
                val density = LocalDensity.current
                val imageSize = (maxWidth * 0.58f).coerceIn(184.dp, 228.dp)
                val travelRangePx = with(density) {
                    (((maxWidth - imageSize) / 2f) - 20.dp).coerceAtLeast(0.dp).toPx()
                }
                val imageOffsetXPx = ((normalizedPatrol - 0.5f) * 2f) * travelRangePx
                val imageOffsetYPx = with(density) {
                    abs(sin(normalizedPatrol * PI * 2)).toFloat() * 10.dp.toPx()
                }
                val imageScale = 1f + (pulsePhase * 0.02f)
                val pulseCircleSize = (imageSize * 1.08f).coerceIn(220.dp, 280.dp)
                val ringCircleSize = (imageSize * 1.3f).coerceIn(260.dp, 320.dp)
                val hamsterFocusX = (normalizedPatrol - 0.5f) * 2f
                val clueChipWidth = (maxWidth * 0.27f).coerceIn(96.dp, 112.dp)

                Box(
                    modifier = Modifier
                        .size(pulseCircleSize)
                        .graphicsLayer {
                            scaleX = 1f + (pulsePhase * 0.08f)
                            scaleY = 1f + (pulsePhase * 0.08f)
                            alpha = 0.18f + ((1f - pulsePhase) * 0.08f)
                        }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )

                Box(
                    modifier = Modifier
                        .size(ringCircleSize)
                        .graphicsLayer {
                            scaleX = 1f + (pulsePhase * 0.06f)
                            scaleY = 1f + (pulsePhase * 0.06f)
                            alpha = 0.26f - (pulsePhase * 0.14f)
                        }
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = CircleShape,
                        ),
                )

                detectiveClueSpecs(clueLabels).forEach { spec ->
                    SearchClueChip(
                        label = spec.label,
                        alignment = spec.alignment,
                        offsetX = spec.offsetX,
                        offsetY = spec.offsetY,
                        chipWidth = clueChipWidth,
                        focusStrength = chipFocusStrength(
                            anchorX = spec.anchorX,
                            hamsterX = hamsterFocusX,
                        ),
                    )
                }

                Image(
                    painter = painter,
                    contentDescription = "구독을 찾는 탐정 햄스터",
                    modifier = Modifier
                        .size(imageSize)
                        .graphicsLayer {
                            translationX = imageOffsetXPx
                            translationY = -imageOffsetYPx
                            scaleX = if (isMovingRight) imageScale else -imageScale
                            scaleY = imageScale
                        },
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.SearchClueChip(
    label: String,
    alignment: Alignment,
    offsetX: Dp,
    offsetY: Dp,
    chipWidth: Dp,
    focusStrength: Float,
) {
    val isFocused = focusStrength > 0.58f
    val chipAlpha = if (isFocused) 1f else (0.78f + (focusStrength * 0.22f)).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .width(chipWidth)
            .heightIn(min = HeroClueChipMinHeight)
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .graphicsLayer {
                alpha = chipAlpha
            },
        shape = PillShape,
        color = if (isFocused) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isFocused) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        ),
        shadowElevation = if (isFocused) 4.dp else 2.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge.copy(
                lineBreak = LineBreak.Paragraph,
            ),
            color = if (isFocused) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun SavingsAmountCard(
    amount: Int,
    hasUsageHistory: Boolean,
    isCollecting: Boolean,
    showAmount: Boolean,
) {
    val spacing = LocalSpacing.current
    val animatedAmount by animateIntAsState(
        targetValue = amount,
        animationSpec = tween(durationMillis = 700),
        label = "savingAmount",
    )

    AppCard(
        contentPadding = PaddingValues(
            horizontal = spacing.space5,
            vertical = spacing.space5,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            Text(
                text = "현재까지 포착한 절약 가능 금액",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                color = AekkimThemeTokens.colors.textSubtle,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (showAmount) formatWon(animatedAmount) else "분석 중",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = if (showAmount) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = HeroDescriptionMinHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        !isCollecting -> "포착이 끝났어요. 상세 결과에서 절약 근거를 바로 확인할 수 있어요."
                        hasUsageHistory -> "최근 사용 흔적이 더 모일수록 금액도 실시간으로 보정되고 있어요."
                        else -> "아직 확정 전이에요. 더 많은 사용 흔적과 결제 단서를 보고 있어요."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineBreak = LineBreak.Paragraph,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AnalysisProgressStrip(
    progress: Float,
    currentStep: AnalysisStep,
    isCollecting: Boolean,
) {
    val spacing = LocalSpacing.current
    val percent = (progress.coerceIn(0f, 1f) * 100).roundToInt()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = currentProgressLabel(currentStep = currentStep, isCollecting = isCollecting),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Text(
            text = progressStatusCopy(currentStep = currentStep, isCollecting = isCollecting),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium.copy(
                lineBreak = LineBreak.Paragraph,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun rememberRotatingMessage(
    currentStep: AnalysisStep,
    isCollecting: Boolean,
): String {
    val messages = remember(currentStep) {
        when (currentStep) {
            AnalysisStep.FetchingPayments -> listOf(
                "결제 기록에서 구독 흔적을 찾는 중이에요",
                "지난 결제 내역에서 반복된 결제를 살펴보고 있어요",
                "정기 결제 후보를 먼저 추려내고 있어요",
            )

            AnalysisStep.MatchingBenefits -> listOf(
                "최근 사용 기록과 결제를 맞춰보고 있어요",
                "최근 앱 사용 흔적을 보고 있어요",
                "사용 빈도와 결제 내역을 함께 확인하고 있어요",
            )

            AnalysisStep.BuildingReport -> listOf(
                "정기 결제 후보를 정리하고 있어요",
                "겹치는 구독과 아낄 수 있는 금액을 계산하고 있어요",
                "마지막으로 구독 후보를 묶고 있어요",
            )
        }
    }
    var index by remember(currentStep) { mutableIntStateOf(0) }

    LaunchedEffect(currentStep, isCollecting) {
        index = 0
        if (!isCollecting) return@LaunchedEffect
        while (true) {
            delay(1_050)
            index = (index + 1) % messages.size
        }
    }

    return messages[index]
}

private fun detectiveCluesForStep(step: AnalysisStep): List<String> {
    return when (step) {
        AnalysisStep.FetchingPayments -> listOf(
            "결제 기록",
            "최근 사용",
            "정기 결제",
            "구독 후보",
        )

        AnalysisStep.MatchingBenefits -> listOf(
            "결제 기록",
            "최근 사용",
            "정기 결제",
            "구독 후보",
        )

        AnalysisStep.BuildingReport -> listOf(
            "결제 기록",
            "최근 사용",
            "정기 결제",
            "최종 후보",
        )
    }
}

private fun defaultDetectiveClues(): List<String> {
    return listOf(
        "결제 기록",
        "최근 사용",
        "정기 결제",
        "구독 후보",
    )
}

private fun detectiveClueSpecs(clueLabels: List<String>): List<HeroClueSpec> {
    val labels = clueLabels.ifEmpty { defaultDetectiveClues() }

    return listOf(
        HeroClueSpec(
            label = labels.getOrElse(0) { "결제 기록" },
            alignment = Alignment.TopStart,
            offsetX = 8.dp,
            offsetY = 34.dp,
            anchorX = -1f,
        ),
        HeroClueSpec(
            label = labels.getOrElse(1) { "최근 사용" },
            alignment = Alignment.TopEnd,
            offsetX = (-8).dp,
            offsetY = 34.dp,
            anchorX = 1f,
        ),
        HeroClueSpec(
            label = labels.getOrElse(2) { "정기 결제" },
            alignment = Alignment.BottomStart,
            offsetX = 12.dp,
            offsetY = (-12).dp,
            anchorX = -1f,
        ),
        HeroClueSpec(
            label = labels.getOrElse(3) { "구독 후보" },
            alignment = Alignment.BottomEnd,
            offsetX = (-12).dp,
            offsetY = (-12).dp,
            anchorX = 1f,
        ),
    )
}

private fun chipFocusStrength(
    anchorX: Float,
    hamsterX: Float,
): Float {
    return (1f - (abs(anchorX - hamsterX) / 2f)).coerceIn(0f, 1f)
}

private fun currentProgressLabel(
    currentStep: AnalysisStep,
    isCollecting: Boolean,
): String {
    if (!isCollecting) return "분석 완료"

    return when (currentStep) {
        AnalysisStep.FetchingPayments -> "결제 기록 포착 중"
        AnalysisStep.MatchingBenefits -> "최근 사용 맞춰보는 중"
        AnalysisStep.BuildingReport -> "정기 결제 정리 중"
    }
}

private fun progressStatusCopy(
    currentStep: AnalysisStep,
    isCollecting: Boolean,
): String {
    if (!isCollecting) return "결과 화면으로 넘어갈 준비가 끝났어요."

    return when (currentStep) {
        AnalysisStep.FetchingPayments -> "숨은 결제와 반복 결제를 먼저 걸러내고 있어요."
        AnalysisStep.MatchingBenefits -> "실제로 쓰는 흔적인지 최근 사용 기록까지 함께 보고 있어요."
        AnalysisStep.BuildingReport -> "절약 근거와 최종 구독 후보를 한 번 더 묶고 있어요."
    }
}

private fun progressFraction(uiState: AnalysisUiState): Float {
    return when {
        uiState.completedSteps.contains(AnalysisStep.BuildingReport) -> 1f
        uiState.currentStep == AnalysisStep.FetchingPayments -> 0.24f
        uiState.currentStep == AnalysisStep.MatchingBenefits -> 0.62f
        uiState.currentStep == AnalysisStep.BuildingReport && uiState.isCollecting -> 0.88f
        else -> 1f
    }
}

private fun formatWon(amount: Int): String {
    return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원"
}

private data class HeroClueSpec(
    val label: String,
    val alignment: Alignment,
    val offsetX: Dp,
    val offsetY: Dp,
    val anchorX: Float,
)
