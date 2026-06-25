package com.ssafy.e106.feature.promotion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.e106.core.ui.component.button.AppButton
import com.ssafy.e106.core.ui.component.card.AppCard
import com.ssafy.e106.core.ui.component.feedback.AppErrorState
import com.ssafy.e106.core.ui.component.image.ServiceLogoBadge
import com.ssafy.e106.core.ui.model.ButtonSize
import com.ssafy.e106.core.ui.model.ButtonVariant
import com.ssafy.e106.core.ui.theme.AekkimThemeTokens
import com.ssafy.e106.core.ui.theme.LocalSpacing
import com.ssafy.e106.feature.promotion.PromotionDetailPriceContentType
import com.ssafy.e106.feature.promotion.PromotionDetailScreenState
import com.ssafy.e106.feature.promotion.PromotionDetailUiModel
import com.ssafy.e106.feature.promotion.PromotionDetailUiState
import com.ssafy.e106.feature.promotion.PromotionPriceSummaryUiModel
import com.ssafy.e106.feature.promotion.PromotionServiceLogoUiModel

@Composable
fun PromotionDetailScreen(
    uiState: PromotionDetailUiState,
    onNavigateBack: () -> Unit,
    onRetryLoad: () -> Unit,
    onOpenSignupLink: () -> Unit,
) {
    val promotion = uiState.screenState.promotionOrNull()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PromotionDetailTopBar(onNavigateBack = onNavigateBack)
        },
        bottomBar = {
            if (promotion != null) {
                PromotionDetailActionBar(
                    screenState = uiState.screenState,
                    onOpenSignupLink = onOpenSignupLink,
                )
            }
        },
    ) { innerPadding ->
        when (val screenState = uiState.screenState) {
            PromotionDetailScreenState.Loading -> {
                PromotionDetailLoadingContent(innerPadding = innerPadding)
            }

            is PromotionDetailScreenState.Error -> {
                PromotionDetailStateContent(innerPadding = innerPadding) {
                    AppErrorState(
                        title = "프로모션 상세 정보를 불러오지 못했어요",
                        description = screenState.message,
                        onRetryClick = onRetryLoad,
                    )
                }
            }

            is PromotionDetailScreenState.Success,
            is PromotionDetailScreenState.Expired,
            is PromotionDetailScreenState.NoLink,
            -> {
                PromotionDetailSuccessContent(
                    screenState = screenState,
                    innerPadding = innerPadding,
                )
            }
        }
    }
}

@Composable
private fun PromotionDetailTopBar(
    onNavigateBack: () -> Unit,
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = spacing.space1, vertical = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "뒤로가기",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun PromotionDetailStateContent(
    innerPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = spacing.screenHorizontalPadding, vertical = spacing.space6),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
        content = content,
    )
}

@Composable
private fun PromotionDetailLoadingContent(
    innerPadding: PaddingValues,
) {
    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(
            start = spacing.screenHorizontalPadding,
            end = spacing.screenHorizontalPadding,
            top = spacing.space2,
            bottom = spacing.space7,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.36f)
                        .height(34.dp),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .height(30.dp),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(30.dp),
                )
            }
        }

        item {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
            )
        }

        item {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .height(28.dp),
            )
        }

        item {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
            )
        }

        item {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
            )
        }

        item {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(24.dp),
            )
        }

        items(count = 3) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            )
        }
    }
}

@Composable
private fun PromotionDetailSuccessContent(
    screenState: PromotionDetailScreenState,
    innerPadding: PaddingValues,
) {
    val promotion = screenState.promotionOrNull() ?: return
    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(
            start = spacing.screenHorizontalPadding,
            end = spacing.screenHorizontalPadding,
            top = spacing.space2,
            bottom = spacing.space7,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
                PromotionSavingsBadge(text = promotion.savingsBadgeText)
                Text(
                    text = promotion.headline,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        item {
            PromotionTargetServicesCard(promotion = promotion)
        }

        item {
            when (promotion.priceContentType) {
                PromotionDetailPriceContentType.PriceComparison -> {
                    PromotionDetailPriceCard(priceSummary = promotion.priceSummary)
                }

                PromotionDetailPriceContentType.SummaryHighlight -> {
                    if (promotion.promotionType != com.ssafy.e106.data.repository.PromotionType.CardBenefit) {
                        PromotionSummaryHighlightCard(
                            message = promotion.priceSummaryDescription,
                            promotionType = promotion.promotionType,
                        )
                    }
                }

                PromotionDetailPriceContentType.None -> Unit
            }
        }

        item {
            PromotionMonthlySavedBanner(message = promotion.monthlySavedDescription)
        }

        item {
            PromotionYearlySavedCard(promotion = promotion)
        }

        promotion.summary?.takeIf {
            it.isNotBlank() &&
                promotion.priceContentType != PromotionDetailPriceContentType.SummaryHighlight &&
                promotion.promotionType != com.ssafy.e106.data.repository.PromotionType.CardBenefit
        }?.let { summary ->
            item {
                PromotionDescriptionCard(
                    title = "프로모션 설명",
                    body = summary,
                    promotionType = promotion.promotionType,
                )
            }
        }

        item {
            PromotionDescriptionCard(
                title = "적용 전에 확인해 주세요",
                body = promotion.detailDescription,
            )
        }

        promotion.conditionDescription?.takeIf { it.isNotBlank() }?.let { condition ->
            item {
                PromotionConditionSection(condition = condition)
            }
        }

        item {
            Text(
                text = "어떻게 적용하나요?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        itemsIndexed(promotion.applySteps) { index, step ->
            PromotionStepCard(
                index = index + 1,
                text = step,
                showDivider = index != promotion.applySteps.lastIndex,
            )
        }
    }
}

@Composable
private fun PromotionTargetServicesCard(
    promotion: PromotionDetailUiModel,
) {
    if (promotion.services.isEmpty() && promotion.validityPeriodLabel.isNullOrBlank() && promotion.imageUrl.isNullOrBlank()) {
        return
    }

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "대상 서비스",
                    style = MaterialTheme.typography.labelLarge,
                    color = AekkimThemeTokens.colors.textMuted,
                )

                if (promotion.services.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        promotion.services.take(3).forEach { service ->
                            PromotionServiceAvatar(service = service)
                        }
                    }

                    Text(
                        text = promotion.services.joinToString(", ") { service -> service.label },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                promotion.validityPeriodLabel?.takeIf { it.isNotBlank() }?.let { validityLabel ->
                    Text(
                        text = validityLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AekkimThemeTokens.colors.textMuted,
                    )
                }
            }

            promotion.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
private fun PromotionServiceAvatar(
    service: PromotionServiceLogoUiModel,
) {
    ServiceLogoBadge(
        serviceName = service.label,
        logoUrl = service.logoUrl,
        serviceCode = service.code,
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.surface,
        border = null,
        contentPadding = 6.dp,
        fallbackTextStyle = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun PromotionSummaryHighlightCard(
    message: String,
    promotionType: com.ssafy.e106.data.repository.PromotionType,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        PromotionSummaryText(
            summary = message,
            promotionType = promotionType,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PromotionDetailPriceCard(
    priceSummary: PromotionPriceSummaryUiModel,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PriceColumn(
                title = "기존 구독료",
                price = priceSummary.originalPriceLabel ?: "-",
                highlight = false,
                strikeThrough = priceSummary.originalPriceLabel != null,
                modifier = Modifier.weight(1f),
            )

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PriceColumn(
                title = "변경 후",
                price = priceSummary.discountPriceLabel ?: "-",
                highlight = true,
                strikeThrough = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PriceColumn(
    title: String,
    price: String,
    highlight: Boolean,
    strikeThrough: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = AekkimThemeTokens.colors.textMuted,
        )
        Text(
            text = price,
            style = if (highlight) {
                MaterialTheme.typography.headlineMedium
            } else {
                MaterialTheme.typography.titleLarge
            },
            color = if (highlight) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (strikeThrough) TextDecoration.LineThrough else null,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PromotionMonthlySavedBanner(
    message: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PromotionYearlySavedCard(
    promotion: PromotionDetailUiModel,
) {
    AppCard(contentPadding = PaddingValues(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = promotion.yearlySavedTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = AekkimThemeTokens.colors.textMuted,
                )
                Text(
                    text = promotion.yearlySavedLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            VerticalDivider(
                modifier = Modifier.height(44.dp),
                color = MaterialTheme.colorScheme.outline,
                thickness = 1.dp,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = promotion.yearlyBenefitDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun PromotionDescriptionCard(
    title: String,
    body: String,
    promotionType: com.ssafy.e106.data.repository.PromotionType? = null,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (promotionType != null) {
                PromotionSummaryText(
                    summary = body,
                    promotionType = promotionType,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun PromotionConditionSection(
    condition: String,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.CreditCard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = condition,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PromotionStepCard(
    index: Int,
    text: String,
    showDivider: Boolean,
) {
    AppCard(contentPadding = PaddingValues(0.dp)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = index.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    text = text,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (showDivider) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun PromotionDetailActionBar(
    screenState: PromotionDetailScreenState,
    onOpenSignupLink: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val isEnabled = screenState is PromotionDetailScreenState.Success

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.screenHorizontalPadding,
                    end = spacing.screenHorizontalPadding,
                    top = spacing.space3,
                    bottom = spacing.space4,
                ),
        ) {
            AppButton(
                text = screenState.actionButtonText(),
                onClick = onOpenSignupLink,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Primary,
                size = ButtonSize.Large,
                enabled = isEnabled,
                accessibilityStateDescription = screenState.actionButtonStateDescription(),
            )
        }
    }
}

@Composable
private fun PromotionSavingsBadge(
    text: String,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = color,
    ) {}
}

private fun PromotionDetailScreenState.promotionOrNull(): PromotionDetailUiModel? {
    return when (this) {
        is PromotionDetailScreenState.Success -> promotion
        is PromotionDetailScreenState.Expired -> promotion
        is PromotionDetailScreenState.NoLink -> promotion
        is PromotionDetailScreenState.Error,
        PromotionDetailScreenState.Loading,
        -> null
    }
}

private fun PromotionDetailScreenState.actionButtonText(): String {
    return when (this) {
        is PromotionDetailScreenState.Success -> promotion.actionButtonText
        is PromotionDetailScreenState.Expired -> "이미 종료된 혜택"
        is PromotionDetailScreenState.NoLink -> "가입 링크 준비 중"
        is PromotionDetailScreenState.Error,
        PromotionDetailScreenState.Loading,
        -> "혜택 적용하러 가기"
    }
}

private fun PromotionDetailScreenState.actionButtonStateDescription(): String? {
    return when (this) {
        is PromotionDetailScreenState.Expired -> "프로모션 종료로 비활성화"
        is PromotionDetailScreenState.NoLink -> "가입 링크 준비 중으로 비활성화"
        is PromotionDetailScreenState.Success,
        is PromotionDetailScreenState.Error,
        PromotionDetailScreenState.Loading,
        -> null
    }
}
