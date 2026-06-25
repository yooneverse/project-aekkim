package com.ssafy.e106.feature.promotion.ui

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.e106.core.ui.component.service.BundleBrandLogoCluster
import com.ssafy.e106.core.ui.component.service.BundleBrandVisualItem
import com.ssafy.e106.core.ui.component.service.resolveBrandGradientColors
import com.ssafy.e106.core.ui.component.button.AppButton
import com.ssafy.e106.core.ui.component.card.AppCard
import com.ssafy.e106.core.ui.component.feedback.AppEmptyState
import com.ssafy.e106.core.ui.component.feedback.AppErrorState
import com.ssafy.e106.core.ui.component.navigation.AppBottomTabBar
import com.ssafy.e106.core.ui.component.navigation.AppBottomTabItem
import com.ssafy.e106.core.ui.model.ButtonSize
import com.ssafy.e106.core.ui.model.ButtonVariant
import com.ssafy.e106.core.ui.theme.AekkimThemeTokens
import com.ssafy.e106.core.ui.theme.LocalSpacing
import com.ssafy.e106.core.ui.theme.PillShape
import com.ssafy.e106.data.repository.PromotionType
import com.ssafy.e106.feature.promotion.PromotionCardUiModel
import com.ssafy.e106.feature.promotion.PromotionListScreenState
import com.ssafy.e106.feature.promotion.PromotionListUiState
import com.ssafy.e106.feature.promotion.PromotionPriceSummaryUiModel
import com.ssafy.e106.feature.promotion.PromotionRecommendationCategoryUiModel
import com.ssafy.e106.feature.promotion.PromotionRecommendationTabUiType
import com.ssafy.e106.feature.promotion.PromotionServiceLogoUiModel

@Composable
fun PromotionListScreen(
    uiState: PromotionListUiState,
    onRetryLoad: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectRecommendationTab: (PromotionRecommendationTabUiType) -> Unit,
    onToggleExpanded: () -> Unit,
    onOpenPromotionDetail: (Long) -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToInsight: () -> Unit,
    onNavigateToMyPage: () -> Unit,
) {
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
                        selected = true,
                        onClick = {},
                    ),
                    AppBottomTabItem(
                        label = "MY",
                        icon = Icons.Outlined.PersonOutline,
                        selected = false,
                        onClick = onNavigateToMyPage,
                    ),
                ),
            )
        },
    ) { innerPadding ->
        when (val screenState = uiState.screenState) {
            PromotionListScreenState.Loading -> PromotionListLoadingContent(
                nickname = uiState.nickname,
                innerPadding = innerPadding,
            )

            PromotionListScreenState.Empty -> PromotionListStateContent(
                nickname = uiState.nickname,
                innerPadding = innerPadding,
            ) {
                AppEmptyState(
                    title = "추천할 혜택이 아직 없어요",
                    description = "카테고리별 추천 데이터가 준비되면 이 화면에서 바로 확인할 수 있어요.",
                )
            }

            is PromotionListScreenState.Error -> PromotionListStateContent(
                nickname = uiState.nickname,
                innerPadding = innerPadding,
            ) {
                AppErrorState(
                    title = "추천 정보를 불러오지 못했어요",
                    description = screenState.message,
                    onRetryClick = onRetryLoad,
                )
            }

            is PromotionListScreenState.Success -> {
                PromotionListSuccessContent(
                    nickname = uiState.nickname,
                    categories = screenState.categories,
                    selectedCategoryKey = uiState.selectedCategoryKey,
                    selectedTab = uiState.selectedTab,
                    isExpanded = uiState.isExpanded,
                    innerPadding = innerPadding,
                    onSelectCategory = onSelectCategory,
                    onSelectRecommendationTab = onSelectRecommendationTab,
                    onToggleExpanded = onToggleExpanded,
                    onOpenPromotionDetail = onOpenPromotionDetail,
                )
            }
        }
    }
}

@Composable
private fun PromotionListSuccessContent(
    nickname: String,
    categories: List<PromotionRecommendationCategoryUiModel>,
    selectedCategoryKey: String?,
    selectedTab: PromotionRecommendationTabUiType,
    isExpanded: Boolean,
    innerPadding: PaddingValues,
    onSelectCategory: (String) -> Unit,
    onSelectRecommendationTab: (PromotionRecommendationTabUiType) -> Unit,
    onToggleExpanded: () -> Unit,
    onOpenPromotionDetail: (Long) -> Unit,
) {
    val spacing = LocalSpacing.current
    val selectedCategory = categories.firstOrNull { it.categoryKey == selectedCategoryKey }
        ?: categories.firstOrNull(PromotionRecommendationCategoryUiModel::hasAnyItems)
        ?: categories.first()
    val availableTabs = selectedCategory.availableTabs
    val resolvedTab = if (availableTabs.contains(selectedTab)) {
        selectedTab
    } else {
        availableTabs.firstOrNull() ?: PromotionRecommendationTabUiType.Bundle
    }
    val selectedItems = selectedCategory.itemsFor(resolvedTab)
    val visibleItems = if (isExpanded) selectedItems else selectedItems.take(DEFAULT_VISIBLE_ITEM_COUNT)
    val canToggleExpanded = selectedItems.size > DEFAULT_VISIBLE_ITEM_COUNT

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = spacing.screenHorizontalPadding,
            end = spacing.screenHorizontalPadding,
            top = spacing.space6,
            bottom = spacing.space6,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
    ) {
        item {
            PromotionListHeader(nickname = nickname)
        }

        item {
            PromotionTabSection(
                title = "카테고리",
                items = categories.map { category -> category.categoryLabel to category.categoryKey },
                selectedKey = selectedCategory.categoryKey,
                onSelect = onSelectCategory,
            )
        }

        item {
            PromotionTabSection(
                title = "추천 유형",
                items = availableTabs.map { tab -> tab.label to tab.name },
                selectedKey = resolvedTab.name,
                onSelect = { key ->
                    availableTabs.firstOrNull { it.name == key }?.let(onSelectRecommendationTab)
                },
            )
        }

        item {
            PromotionSelectedHeader(
                categoryLabel = selectedCategory.categoryLabel,
                tabLabel = resolvedTab.label,
                count = selectedItems.size,
                isExpanded = isExpanded,
                canToggleExpanded = canToggleExpanded,
            )
        }

        if (selectedItems.isEmpty()) {
            item {
                AppEmptyState(
                    title = "추천 항목이 없어요",
                    description = "${selectedCategory.categoryLabel} 카테고리의 ${resolvedTab.label} 추천이 준비되면 여기에 표시돼요.",
                )
            }
        } else {
            items(visibleItems, key = { item -> item.promotionId }) { card ->
                PromotionRecommendationCard(
                    card = card,
                    onClick = { onOpenPromotionDetail(card.promotionId) },
                )
            }

            if (canToggleExpanded) {
                item {
                    PromotionExpandToggleSection(
                        isExpanded = isExpanded,
                        totalCount = selectedItems.size,
                        onToggleExpanded = onToggleExpanded,
                    )
                }
            }
        }
    }
}

@Composable
private fun PromotionListStateContent(
    nickname: String,
    innerPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .statusBarsPadding()
            .padding(horizontal = spacing.screenHorizontalPadding, vertical = spacing.space6),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
    ) {
        PromotionListHeader(nickname = nickname)
        content()
    }
}

@Composable
private fun PromotionListLoadingContent(
    nickname: String,
    innerPadding: PaddingValues,
) {
    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = spacing.screenHorizontalPadding,
            end = spacing.screenHorizontalPadding,
            top = spacing.space6,
            bottom = spacing.space6,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
    ) {
        item {
            PromotionListHeader(nickname = nickname)
        }

        item {
            LoadingPill(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(20.dp),
            )
        }

        items(count = 3) {
            PromotionCardSkeleton()
        }
    }
}

@Composable
private fun PromotionListHeader(
    nickname: String,
) {
    val spacing = LocalSpacing.current
    val displayName = nickname.ifBlank { "사용자" }
    val headerText = buildAnnotatedString {
        append("지금 ")
        pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
        append(displayName)
        pop()
        append("님께 맞는 추천을 모아봤어요")
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text(
            text = headerText,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "카테고리별로 번들, 프로모션, 카드 혜택을 나눠서 확인할 수 있어요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PromotionTabSection(
    title: String,
    items: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    val spacing = LocalSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
            items(items, key = { it.second }) { item ->
                PromotionTabChip(
                    label = item.first,
                    selected = item.second == selectedKey,
                    onClick = { onSelect(item.second) },
                )
            }
        }
    }
}

@Composable
private fun PromotionTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun PromotionSelectedHeader(
    categoryLabel: String,
    tabLabel: String,
    count: Int,
    isExpanded: Boolean,
    canToggleExpanded: Boolean,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when (tabLabel) {
                    "번들" -> Icons.Outlined.Subscriptions
                    "카드 혜택" -> Icons.Outlined.CreditCard
                    else -> Icons.Outlined.LocalOffer
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "$categoryLabel · $tabLabel",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${count}개",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (canToggleExpanded && !isExpanded) {
            Text(
                text = "상위 3개만 먼저 보여드려요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PromotionExpandToggleSection(
    isExpanded: Boolean,
    totalCount: Int,
    onToggleExpanded: () -> Unit,
) {
    val remainingCount = (totalCount - DEFAULT_VISIBLE_ITEM_COUNT).coerceAtLeast(0)
    val buttonText = if (isExpanded) {
        "추천 접기"
    } else {
        "추천 ${remainingCount}개 더 보기"
    }

    AppButton(
        text = buttonText,
        onClick = onToggleExpanded,
        modifier = Modifier.fillMaxWidth(),
        variant = ButtonVariant.Secondary,
        size = ButtonSize.Medium,
    )
}

@Composable
private fun PromotionRecommendationCard(
    card: PromotionCardUiModel,
    onClick: () -> Unit,
) {
    val spacing = LocalSpacing.current

    AppCard(onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            PromotionHeroImage(
                imageUrl = card.imageUrl,
                services = card.services,
                badgeText = card.savingsBadgeText,
                promotionType = card.promotionType,
            )

            Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                Text(
                    text = card.headline,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                RecommendationReasonChips(reasons = card.recommendationReasons)
                card.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                    PromotionSummaryText(
                        summary = summary,
                        promotionType = card.promotionType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            PromotionPriceComparison(priceSummary = card.priceSummary)

            card.conditionDescription?.takeIf { it.isNotBlank() }?.let { condition ->
                PromotionConditionCard(condition = condition)
            }
        }
    }
}

@Composable
private fun RecommendationReasonChips(
    reasons: List<String>,
) {
    val primaryReason = reasons.firstOrNull()?.takeIf { it.isNotBlank() } ?: return

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary,
    ) {
        Text(
            text = primaryReason,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PromotionHeroImage(
    imageUrl: String?,
    services: List<PromotionServiceLogoUiModel>,
    badgeText: String,
    promotionType: PromotionType,
) {
    val brandVisuals = remember(services) { services.toBundleBrandVisuals() }
    val gradientColors = brandVisuals.resolveBrandGradientColors(
        defaultStart = MaterialTheme.colorScheme.surfaceVariant,
        defaultEnd = MaterialTheme.colorScheme.surface,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(MaterialTheme.shapes.large)
            .background(brush = Brush.linearGradient(colors = gradientColors)),
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (promotionType == PromotionType.Bundle) 0.24f else 0.94f,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.08f)),
                    ),
                )
                .padding(12.dp),
        ) {
            PromotionServiceLogoRow(
                services = services,
                modifier = Modifier.align(Alignment.Center),
            )

            PromotionSavingsBadge(
                text = badgeText,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun PromotionImageFallback(
    services: List<PromotionServiceLogoUiModel>,
) {
    val gradientColors = services.resolveFallbackGradientColors(
        defaultStart = MaterialTheme.colorScheme.surfaceVariant,
        defaultEnd = MaterialTheme.colorScheme.surface,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = gradientColors)),
    )
}

private fun List<PromotionServiceLogoUiModel>.resolveFallbackGradientColors(
    defaultStart: Color,
    defaultEnd: Color,
): List<Color> {
    val paletteByService = this
        .map { service -> service.resolveBrandPalette() }
        .filter { palette -> palette.isNotEmpty() }
        .take(2)

    val brandColors = paletteByService
        .flatMap { palette -> palette }
        .distinct()

    return when {
        brandColors.size >= 2 -> brandColors.take(4)
        brandColors.size == 1 -> listOf(brandColors.first(), defaultEnd)
        else -> listOf(defaultStart, defaultEnd)
    }
}

private fun PromotionServiceLogoUiModel.resolveBrandPalette(): List<Color> {
    val serviceKey = listOfNotNull(code, label)
        .joinToString(separator = " ")
        .lowercase()

    return when {
        "netflix" in serviceKey || "넷플릭스" in serviceKey -> listOf(
            Color(0xFF0F0F0F),
            Color(0xFFE50914),
        )

        "tving" in serviceKey || "티빙" in serviceKey -> listOf(
            Color(0xFFFFFFFF),
            Color(0xFFFF2E2E),
        )

        "wavve" in serviceKey || "웨이브" in serviceKey -> listOf(
            Color(0xFF0A163D),
            Color(0xFF3D7CFF),
        )

        "disney" in serviceKey || "디즈니" in serviceKey -> listOf(
            Color(0xFF0B1D3A),
            Color(0xFF00E2FF),
        )

        "watcha" in serviceKey || "왓챠" in serviceKey -> listOf(
            Color(0xFF111111),
            Color(0xFFFF2F6E),
        )

        "youtube" in serviceKey || "유튜브" in serviceKey -> listOf(
            Color(0xFFFFFFFF),
            Color(0xFFFF0000),
        )

        "spotify" in serviceKey || "스포티파이" in serviceKey -> listOf(
            Color(0xFF121212),
            Color(0xFF1ED760),
        )

        "apple" in serviceKey || "애플" in serviceKey -> listOf(
            Color(0xFF111111),
            Color(0xFF6E6E73),
        )

        "naver" in serviceKey || "네이버" in serviceKey -> listOf(
            Color(0xFF03C75A),
            Color(0xFF0A2E1B),
        )

        "coupang" in serviceKey || "쿠팡" in serviceKey -> listOf(
            Color(0xFF2A58FF),
            Color(0xFFFF5555),
        )

        else -> emptyList()
    }
}

@Composable
private fun PromotionServiceLogoRow(
    services: List<PromotionServiceLogoUiModel>,
    modifier: Modifier = Modifier,
) {
    BundleBrandLogoCluster(
        services = services.toBundleBrandVisuals(),
        modifier = modifier,
        logoSize = 38.dp,
        overlap = 8.dp,
    )
}

private fun List<PromotionServiceLogoUiModel>.toBundleBrandVisuals(): List<BundleBrandVisualItem> {
    return map { service ->
        BundleBrandVisualItem(
            name = service.label,
            code = service.code,
            logoUrl = service.logoUrl,
        )
    }
}

@Composable
private fun PromotionSavingsBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = PillShape,
        color = Color.Black.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shadowElevation = 1.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.96f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PromotionPriceComparison(
    priceSummary: PromotionPriceSummaryUiModel,
) {
    if (!priceSummary.hasPriceComparison) {
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = priceSummary.originalPriceLabel.orEmpty(),
            style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.LineThrough),
            color = AekkimThemeTokens.colors.textMuted,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = AekkimThemeTokens.colors.textMuted,
        )
        Text(
            text = priceSummary.discountPriceLabel.orEmpty(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private const val DEFAULT_VISIBLE_ITEM_COUNT = 3

@Composable
private fun PromotionConditionCard(
    condition: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
private fun PromotionCardSkeleton() {
    val spacing = LocalSpacing.current

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            LoadingPill(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
                shapeColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            LoadingPill(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(22.dp),
            )
            LoadingPill(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(18.dp),
            )
            LoadingPill(
                modifier = Modifier
                    .fillMaxWidth(0.54f)
                    .height(28.dp),
            )
            LoadingPill(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            )
        }
    }
}

@Composable
private fun LoadingPill(
    modifier: Modifier = Modifier,
    shapeColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Surface(
        modifier = modifier.heightIn(min = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = shapeColor,
    ) {}
}
