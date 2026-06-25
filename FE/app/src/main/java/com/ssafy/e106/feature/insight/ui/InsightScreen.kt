package com.ssafy.e106.feature.insight.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.e106.core.ui.component.card.AppCard
import com.ssafy.e106.core.ui.component.feedback.AppEmptyState
import com.ssafy.e106.core.ui.component.feedback.AppErrorState
import com.ssafy.e106.core.ui.component.feedback.AppLoadingState
import com.ssafy.e106.core.ui.component.image.ServiceLogoBadge
import com.ssafy.e106.core.ui.component.navigation.AppBottomTabBar
import com.ssafy.e106.core.ui.component.navigation.AppBottomTabItem
import com.ssafy.e106.core.ui.component.service.BundleBrandLogoCluster
import com.ssafy.e106.core.ui.component.service.BundleBrandVisualItem
import com.ssafy.e106.core.ui.theme.AekkimThemeTokens
import com.ssafy.e106.core.ui.theme.LocalSpacing
import com.ssafy.e106.feature.analysis.model.ServiceCatalog
import com.ssafy.e106.feature.analysis.model.TrackedSubscriptionPackages
import com.ssafy.e106.feature.insight.InsightReportUiModel
import com.ssafy.e106.feature.insight.InsightScreenState
import com.ssafy.e106.feature.insight.InsightSubscriptionItemUiModel
import com.ssafy.e106.feature.insight.InsightUiState
import com.ssafy.e106.feature.insight.formatInsightMinutes
import com.ssafy.e106.feature.insight.formatInsightWon

private val insightBundleCatalog = TrackedSubscriptionPackages.toServiceCatalog()
private val insightHighlightRegexes = buildList {
    insightBundleCatalog.services.forEach { service ->
        add(service.name)
        addAll(service.aliases)
    }
    insightBundleCatalog.bundles.forEach { bundle ->
        add(bundle.name)
        addAll(bundle.aliases)
    }
    addAll(
        listOf(
            "더 자주",
            "덜 자주",
            "가장 자주",
            "더 오래",
            "덜 오래",
            "가장 오래",
            "더 많이",
            "더 적게",
            "가장 많이",
            "가장 적게",
            "늘었어요",
            "줄었어요",
            "높아요",
            "낮아요",
        ),
    )
}.map(String::trim)
    .filter { term -> term.length >= 2 }
    .distinct()
    .sortedByDescending(String::length)
    .map { term -> Regex(Regex.escape(term), RegexOption.IGNORE_CASE) }
private val insightMetricRegex = Regex("""\d[\d,]*(?:\.\d+)?(?:시간|분|일|개월|원|회|건|배|%)?""")

@Composable
fun InsightScreen(
    uiState: InsightUiState,
    targetSubscriptionId: Long?,
    onRetryLoad: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToPromotionList: () -> Unit,
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
                        selected = true,
                        onClick = {},
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
                        selected = false,
                        onClick = onNavigateToMyPage,
                    ),
                ),
            )
        },
    ) { innerPadding ->
        when (val screenState = uiState.screenState) {
            InsightScreenState.Loading -> InsightStateContent(innerPadding = innerPadding) {
                AppLoadingState(
                    title = "인사이트를 정리하는 중이에요.",
                    description = "최근 30일 사용 순위를 정리하고 있어요.",
                )
            }

            InsightScreenState.Empty -> InsightStateContent(innerPadding = innerPadding) {
                AppEmptyState(
                    title = "아직 보여줄 인사이트가 없어요",
                    description = "사용량 데이터가 쌓이면 이 화면에서 바로 확인할 수 있어요.",
                )
            }

            is InsightScreenState.Error -> InsightStateContent(innerPadding = innerPadding) {
                AppErrorState(
                    title = "인사이트를 불러오지 못했어요",
                    description = screenState.message,
                    onRetryClick = onRetryLoad,
                )
            }

            is InsightScreenState.Success -> InsightSuccessContent(
                report = screenState.report,
                targetSubscriptionId = targetSubscriptionId,
                innerPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun InsightStateContent(
    innerPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    val spacing = LocalSpacing.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .statusBarsPadding()
            .padding(horizontal = spacing.screenHorizontalPadding, vertical = spacing.space6),
    ) {
        content()
    }
}

@Composable
private fun InsightSuccessContent(
    report: InsightReportUiModel,
    targetSubscriptionId: Long?,
    innerPadding: PaddingValues,
) {
    val spacing = LocalSpacing.current
    val listState = rememberLazyListState()

    LaunchedEffect(report.items, report.relatedInsights, targetSubscriptionId) {
        scrollToTargetSubscription(
            listState = listState,
            report = report,
            targetSubscriptionId = targetSubscriptionId,
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .statusBarsPadding(),
        state = listState,
        contentPadding = PaddingValues(
            start = spacing.screenHorizontalPadding,
            end = spacing.screenHorizontalPadding,
            top = spacing.space6,
            bottom = spacing.space6,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap),
    ) {
        item { InsightHeader() }
        item { OverallUsageCard(report = report) }
        if (report.relatedInsights.isNotEmpty()) {
            item { RelatedInsightsCard(insights = report.relatedInsights) }
        }
        item {
            Text(
                text = "구독별 사용 현황",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        items(report.items, key = { item -> item.subscriptionId }) { item ->
            InsightSubscriptionCard(item = item)
        }
    }
}

private suspend fun scrollToTargetSubscription(
    listState: LazyListState,
    report: InsightReportUiModel,
    targetSubscriptionId: Long?,
) {
    val subscriptionId = targetSubscriptionId ?: return
    val targetItemIndex = report.items.indexOfFirst { item -> item.subscriptionId == subscriptionId }
    if (targetItemIndex < 0) return

    val staticItemCount = 3 + if (report.relatedInsights.isNotEmpty()) 1 else 0
    listState.animateScrollToItem(staticItemCount + targetItemIndex)
}

@Composable
private fun InsightHeader() {
    val spacing = LocalSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Text(
            text = "인사이트",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "최근 30일 사용 순위를 기준으로 구독 상태를 정리했어요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun OverallUsageCard(
    report: InsightReportUiModel,
) {
    val spacing = LocalSpacing.current
    val podiumItems = report.items
        .sortedByDescending { item -> item.totalUsedMinutes }
        .take(3)
    val topItem = podiumItems.firstOrNull()

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                Text(
                    text = if (topItem != null) {
                        "지난 ${report.summary.windowDays}일 동안 ${topItem.serviceName} 사용 시간이 가장 길었어요"
                    } else {
                        "지난 ${report.summary.windowDays}일 사용 순위를 아직 집계하지 못했어요"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "최근 30일 기준으로 많이 본 구독 3개를 비교할 수 있어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (podiumItems.isEmpty()) {
                Text(
                    text = "아직 집계된 사용 기록이 없어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                TopThreePodiumCard(items = podiumItems)
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = AekkimThemeTokens.colors.textMuted,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = valueColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun TopThreePodiumCard(
    items: List<InsightSubscriptionItemUiModel>,
) {
    val second = items.getOrNull(1)
    val first = items.getOrNull(0)
    val third = items.getOrNull(2)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        PodiumCard(
            item = second,
            rank = 2,
            height = 184.dp,
            highlight = false,
            modifier = Modifier.weight(1f, fill = false),
        )
        PodiumCard(
            item = first,
            rank = 1,
            height = 224.dp,
            highlight = true,
            modifier = Modifier.weight(1f, fill = false),
        )
        PodiumCard(
            item = third,
            rank = 3,
            height = 176.dp,
            highlight = false,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun PodiumCard(
    item: InsightSubscriptionItemUiModel?,
    rank: Int,
    height: androidx.compose.ui.unit.Dp,
    highlight: Boolean,
    modifier: Modifier = Modifier,
) {
    if (item == null) {
        Box(
            modifier = modifier
                .width(104.dp)
                .height(height),
        )
        return
    }

    val containerColor = if (highlight) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = if (highlight) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    }
    val rankLabel = "${rank}위"

    Surface(
        modifier = modifier
            .width(112.dp)
            .height(height),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (highlight) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                },
            ) {
                Text(
                    text = rankLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (highlight) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Box(modifier = Modifier.height(12.dp))
            InsightSubscriptionBrandLeading(
                item = item,
                singleLogoSize = if (highlight) 52.dp else 46.dp,
                clusterLogoSize = if (highlight) 30.dp else 28.dp,
                clusterWidth = if (highlight) 72.dp else 64.dp,
            )
            Box(modifier = Modifier.height(12.dp))
            Text(
                text = item.serviceName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Box(modifier = Modifier.height(10.dp))
            Text(
                text = formatInsightMinutes(item.totalUsedMinutes),
                style = if (highlight) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = if (highlight) {
                    MaterialTheme.colorScheme.primary
                } else {
                    AekkimThemeTokens.colors.textSubtle
                },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun InsightSubscriptionBrandLeading(
    item: InsightSubscriptionItemUiModel,
    singleLogoSize: Dp,
    clusterLogoSize: Dp,
    clusterWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val bundleVisuals = item.resolveBundleBrandVisuals()
    val bundleBorder = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
    )

    Box(
        modifier = modifier.width(clusterWidth),
        contentAlignment = Alignment.Center,
    ) {
        if (item.subscriptionType.equals("BUNDLE", ignoreCase = true) && bundleVisuals.size > 1) {
            BundleBrandLogoCluster(
                services = bundleVisuals,
                logoSize = clusterLogoSize,
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
                modifier = Modifier.size(singleLogoSize),
                shape = MaterialTheme.shapes.large,
            )
        }
    }
}

internal fun InsightSubscriptionItemUiModel.resolveBundleBrandVisuals(
    bundleCatalog: ServiceCatalog = insightBundleCatalog,
): List<BundleBrandVisualItem> {
    if (!subscriptionType.equals("BUNDLE", ignoreCase = true)) return emptyList()

    val bundle = bundleCatalog.resolveBundleForInsightItem(this) ?: return emptyList()

    return bundle.serviceCodes.mapNotNull { serviceCode ->
        bundleCatalog.findServiceByCode(serviceCode)?.let { service ->
            BundleBrandVisualItem(
                name = service.name,
                code = service.code,
                logoUrl = service.logoUrl,
            )
        }
    }
}

private fun ServiceCatalog.resolveBundleForInsightItem(
    item: InsightSubscriptionItemUiModel,
): ServiceCatalog.Bundle? {
    val rawCandidates = item.bundleLookupCandidates()
    rawCandidates.firstNotNullOfOrNull { candidate -> findBundleByCode(candidate) }?.let { return it }

    val normalizedCandidates = rawCandidates
        .map(::normalizeInsightBundleLookupToken)
        .filter { candidate -> candidate.length >= 2 }
    if (normalizedCandidates.isEmpty()) return null

    return bundles
        .map { bundle -> bundle to bundle.matchInsightBundleCandidates(normalizedCandidates) }
        .filter { (_, score) -> score > 0 }
        .maxByOrNull { (_, score) -> score }
        ?.first
}

private fun InsightSubscriptionItemUiModel.bundleLookupCandidates(): List<String> {
    return listOfNotNull(bundleCode, serviceName, planName)
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
}

private fun ServiceCatalog.Bundle.matchInsightBundleCandidates(
    normalizedCandidates: List<String>,
): Int {
    val normalizedTokens = buildSet {
        add(code)
        add(name)
        addAll(aliases)
        addAll(plans.map(ServiceCatalog.BundlePlan::planName))
    }.map(::normalizeInsightBundleLookupToken)
        .filter { token -> token.length >= 2 }
        .toSet()

    return normalizedCandidates.sumOf { candidate ->
        normalizedTokens.maxOfOrNull { token ->
            when {
                candidate == token -> 6
                candidate.contains(token) || token.contains(candidate) -> 3
                else -> 0
            }
        } ?: 0
    }
}

private fun normalizeInsightBundleLookupToken(value: String): String {
    return value
        .trim()
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]"), "")
}

@Composable
private fun RelatedInsightsCard(
    insights: List<String>,
) {
    val spacing = LocalSpacing.current
    val highlightColor = MaterialTheme.colorScheme.primary

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            Text(
                text = "함께 보면 좋은 해석",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                insights.forEach { insight ->
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = buildHighlightedInsightText(
                                message = insight,
                                highlightColor = highlightColor,
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

private fun buildHighlightedInsightText(
    message: String,
    highlightColor: Color,
): AnnotatedString {
    val highlightRanges = collectInsightHighlightRanges(message)
    if (highlightRanges.isEmpty()) {
        return AnnotatedString(message)
    }

    return buildAnnotatedString {
        var cursor = 0

        highlightRanges.forEach { range ->
            if (cursor < range.first) {
                append(message.substring(cursor, range.first))
            }

            withStyle(
                SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.SemiBold,
                ),
            ) {
                append(message.substring(range.first, range.last + 1))
            }

            cursor = range.last + 1
        }

        if (cursor < message.length) {
            append(message.substring(cursor))
        }
    }
}

private fun collectInsightHighlightRanges(message: String): List<IntRange> {
    if (message.isBlank()) return emptyList()

    val matches = buildList {
        insightHighlightRegexes.forEach { regex ->
            regex.findAll(message).forEach { match -> add(match.range) }
        }
        insightMetricRegex.findAll(message).forEach { match -> add(match.range) }
    }

    if (matches.isEmpty()) return emptyList()

    val sortedMatches = matches.sortedWith(compareBy<IntRange> { it.first }.thenBy { it.last })
    val mergedMatches = mutableListOf<IntRange>()
    var currentRange = sortedMatches.first()

    sortedMatches.drop(1).forEach { nextRange ->
        currentRange = if (nextRange.first <= currentRange.last + 1) {
            currentRange.first..maxOf(currentRange.last, nextRange.last)
        } else {
            mergedMatches += currentRange
            nextRange
        }
    }

    mergedMatches += currentRange
    return mergedMatches
}

@Composable
private fun InsightSubscriptionCard(
    item: InsightSubscriptionItemUiModel,
) {
    val spacing = LocalSpacing.current

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InsightSubscriptionBrandLeading(
                    item = item,
                    singleLogoSize = 48.dp,
                    clusterLogoSize = 28.dp,
                    clusterWidth = 68.dp,
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
                        text = item.planName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AekkimThemeTokens.colors.textSubtle,
                    )
                }
                Text(
                    text = formatInsightWon(item.monthlyPrice),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "최근 30일 사용",
                    value = formatInsightMinutes(item.totalUsedMinutes),
                    valueColor = MaterialTheme.colorScheme.primary,
                )
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "사용한 날",
                    value = "${item.usedDays}일",
                    valueColor = MaterialTheme.colorScheme.primary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                item.hourlyCost?.let { hourlyCost ->
                    SummaryMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "시간당 비용",
                        value = formatInsightWon(hourlyCost),
                        valueColor = MaterialTheme.colorScheme.primary,
                    )
                }
                item.lastUsedDateLabel?.let { label ->
                    SummaryMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "마지막 사용",
                        value = label,
                        valueColor = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            item.nudgeMessage?.let { message ->
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
