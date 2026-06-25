package com.ssafy.e106.feature.dashboard.ui.component

import android.content.res.Configuration

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.e106.core.ui.component.button.AppButton
import com.ssafy.e106.core.ui.component.card.AppCard
import com.ssafy.e106.core.ui.component.feedback.AppEmptyState
import com.ssafy.e106.core.ui.component.feedback.AppErrorState
import com.ssafy.e106.core.ui.component.feedback.AppLoadingState
import com.ssafy.e106.core.ui.component.image.ServiceLogoBadge
import com.ssafy.e106.core.ui.component.input.AppTextField
import com.ssafy.e106.core.ui.component.service.BundleBrandLogoCluster
import com.ssafy.e106.core.ui.component.service.BundleBrandVisualItem
import com.ssafy.e106.core.ui.component.service.ServiceBrandIcon
import com.ssafy.e106.core.ui.component.service.resolveBrandGradientColors
import com.ssafy.e106.core.ui.component.sheet.AppBottomSheet
import com.ssafy.e106.core.ui.model.ButtonSize
import com.ssafy.e106.core.ui.model.ButtonVariant
import com.ssafy.e106.core.ui.theme.LocalSpacing
import com.ssafy.e106.core.ui.theme.PillShape
import com.ssafy.e106.data.repository.PromotionType
import com.ssafy.e106.feature.dashboard.DashboardBundleItem
import com.ssafy.e106.feature.dashboard.DashboardPlanItem
import com.ssafy.e106.feature.dashboard.DashboardServiceItem
import com.ssafy.e106.feature.dashboard.ManualAddStep
import com.ssafy.e106.feature.dashboard.RecommendationUiItem
import com.ssafy.e106.feature.dashboard.RecommendationServiceLogoUiItem
import com.ssafy.e106.feature.dashboard.SubscriptionDetailUiModel
import com.ssafy.e106.feature.dashboard.SubscriptionDetailUsagePoint
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ManualAddBottomSheet(
    manualAddStep: ManualAddStep,
    searchQuery: String,
    services: List<DashboardServiceItem>,
    serviceSearchResults: List<DashboardServiceItem>,
    bundleSearchResults: List<DashboardBundleItem>,
    selectedService: DashboardServiceItem?,
    selectedBundle: DashboardBundleItem?,
    planOptions: List<DashboardPlanItem>,
    selectedPlanId: Long?,
    selectedBillingDay: Int?,
    selectedBillingDate: LocalDate?,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectService: (Long) -> Unit,
    onSelectBundle: (String) -> Unit,
    onSelectPlan: (Long) -> Unit,
    onSelectBillingDay: (Int) -> Unit,
    onSelectBillingDate: (LocalDate) -> Unit,
    onSubmit: () -> Unit,
) {
    val selectedPlan = planOptions.firstOrNull { it.servicePlanId == selectedPlanId }
    val isYearlyPlan = selectedPlan?.isYearly() == true
    val isYearlyBundle = selectedBundle?.isYearly() == true
    val isConfigurationStep = manualAddStep == ManualAddStep.ConfigurePlanAndBillingDay ||
        manualAddStep == ManualAddStep.ConfigureBundleAndBillingDay

    LaunchedEffect(manualAddStep, selectedPlanId, selectedBundle?.code, selectedBillingDay) {
        val requiresDayPicker = when (manualAddStep) {
            ManualAddStep.ConfigurePlanAndBillingDay -> !isYearlyPlan
            ManualAddStep.ConfigureBundleAndBillingDay -> !isYearlyBundle
            else -> false
        }
        if (isConfigurationStep && requiresDayPicker && selectedBillingDay == null) {
            onSelectBillingDay(1)
        }
    }

    AppBottomSheet(
        onDismiss = onDismiss,
        title = when (manualAddStep) {
            ManualAddStep.SearchCatalog -> "구독 추가"
            ManualAddStep.SelectService -> "서비스를 선택해주세요"
            ManualAddStep.ConfigurePlanAndBillingDay -> if (isEditing) {
                "구독 수정하기"
            } else {
                "요금제와 결제일을 선택해주세요"
            }
            ManualAddStep.ConfigureBundleAndBillingDay -> if (isEditing) {
                "번들 구독 수정하기"
            } else {
                null
            }
        },
        footerContent = if (isConfigurationStep) {
            {
                AppButton(
                    text = if (isEditing) "수정 완료" else "완료",
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Primary,
                    size = ButtonSize.Large,
                    enabled = when (manualAddStep) {
                        ManualAddStep.ConfigureBundleAndBillingDay -> {
                            selectedBundle != null &&
                                if (isYearlyBundle) selectedBillingDate != null else selectedBillingDay != null
                        }
                        ManualAddStep.ConfigurePlanAndBillingDay -> {
                            selectedPlanId != null &&
                                if (isYearlyPlan) selectedBillingDate != null else selectedBillingDay != null
                        }
                        else -> false
                    },
                )
            }
        } else {
            null
        },
    ) {
        when (manualAddStep) {
            ManualAddStep.SearchCatalog -> {
                SearchCatalogStep(
                    searchQuery = searchQuery,
                    serviceResults = serviceSearchResults,
                    bundleResults = bundleSearchResults,
                    onSearchQueryChange = onSearchQueryChange,
                    onSelectService = onSelectService,
                    onSelectBundle = onSelectBundle,
                )
            }

            ManualAddStep.SelectService -> {
                ServiceSelectionBody(
                    services = services,
                    onSelectService = onSelectService,
                )
            }

            ManualAddStep.ConfigurePlanAndBillingDay -> {
                val service = selectedService ?: return@AppBottomSheet
                ServiceConfigurationStep(
                    service = service,
                    planOptions = planOptions,
                    selectedPlanId = selectedPlanId,
                    selectedBillingDay = selectedBillingDay,
                    selectedBillingDate = selectedBillingDate,
                    onSelectPlan = onSelectPlan,
                    onSelectBillingDay = onSelectBillingDay,
                    onSelectBillingDate = onSelectBillingDate,
                )
            }

            ManualAddStep.ConfigureBundleAndBillingDay -> {
                val bundle = selectedBundle ?: return@AppBottomSheet
                BundleConfigurationStep(
                    bundle = bundle,
                    selectedBillingDay = selectedBillingDay,
                    selectedBillingDate = selectedBillingDate,
                    onSelectBillingDay = onSelectBillingDay,
                    onSelectBillingDate = onSelectBillingDate,
                )
            }
        }
    }
}

@Composable
private fun SearchCatalogStep(
    searchQuery: String,
    serviceResults: List<DashboardServiceItem>,
    bundleResults: List<DashboardBundleItem>,
    onSearchQueryChange: (String) -> Unit,
    onSelectService: (Long) -> Unit,
    onSelectBundle: (String) -> Unit,
) {
    val spacing = LocalSpacing.current

    AppTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = "서비스명이나 번들을 검색해보세요",
        helperText = "서비스명, 번들명, 포함 서비스 이름 기준으로 바로 검색돼요.",
        leadingIcon = Icons.Outlined.Search,
    )

    when {
        serviceResults.isEmpty() && bundleResults.isEmpty() -> {
            AppEmptyState(
                title = "검색 결과가 없어요.",
                description = "서비스명이나 번들 구성 서비스 이름으로 다시 검색해 보세요.",
            )
        }

        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.space3),
                contentPadding = PaddingValues(bottom = spacing.space1),
            ) {
                if (serviceResults.isNotEmpty()) {
                    item {
                        SearchSectionHeader(
                            title = "서비스",
                            count = serviceResults.size,
                        )
                    }
                    items(
                        items = serviceResults,
                        key = { service -> "service-${service.serviceId}" },
                    ) { service ->
                        ServiceSearchResultCard(
                            service = service,
                            onClick = { onSelectService(service.serviceId) },
                        )
                    }
                }

                if (bundleResults.isNotEmpty()) {
                    item {
                        SearchSectionHeader(
                            title = "번들 상품",
                            count = bundleResults.size,
                        )
                    }
                    items(
                        items = bundleResults,
                        key = { bundle -> "bundle-${bundle.code}" },
                    ) { bundle ->
                        BundleSearchResultCard(
                            bundle = bundle,
                            onClick = { onSelectBundle(bundle.code) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    count: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "${count}개",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ServiceSearchResultCard(
    service: DashboardServiceItem,
    onClick: () -> Unit,
) {
    val spacing = LocalSpacing.current

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = spacing.space4, vertical = spacing.space4),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ServiceLogoBadge(
                serviceName = service.name,
                logoUrl = service.logoUrl,
                serviceCode = service.code,
                modifier = Modifier.size(48.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "단일 서비스",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ResultTypePill(text = "서비스")
        }
    }
}

@Composable
private fun BundleSearchResultCard(
    bundle: DashboardBundleItem,
    onClick: () -> Unit,
) {
    val spacing = LocalSpacing.current

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = spacing.space4, vertical = spacing.space4),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            BundleIncludedServicesHero(
                services = bundle.includedServices,
                logoSize = 38.dp,
                minHeight = 112.dp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = bundle.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "포함 서비스 ${bundle.includedServiceNames()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ResultTypePill(text = "번들")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "고정 월 금액",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatWon(bundle.monthlyPrice),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ResultTypePill(text: String) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlanOptionPill(
    text: String,
    emphasized: Boolean = false,
) {
    val containerColor = if (emphasized) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (emphasized) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = PillShape,
        color = containerColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

@Composable
private fun ServiceSelectionBody(
    services: List<DashboardServiceItem>,
    onSelectService: (Long) -> Unit,
) {
    val spacing = LocalSpacing.current

    Text(
        text = "현재 제공 중인 구독 서비스를 먼저 선택해주세요.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (services.isEmpty()) {
        AppEmptyState(
            title = "선택할 수 있는 서비스가 없어요.",
            description = "서비스 정보를 다시 불러온 뒤 시도해주세요.",
        )
    } else {
        services.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                rowItems.forEach { service ->
                    AppCard(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = spacing.space4),
                        onClick = { onSelectService(service.serviceId) },
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(spacing.space2),
                        ) {
                            ServiceBrandIcon(
                                serviceName = service.name,
                                logoUrl = service.logoUrl,
                                serviceCode = service.code,
                                modifier = Modifier.size(52.dp),
                            )
                            Text(
                                text = service.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                repeat(3 - rowItems.size) {
                    SpacerCardPlaceholder(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ServiceConfigurationStep(
    service: DashboardServiceItem,
    planOptions: List<DashboardPlanItem>,
    selectedPlanId: Long?,
    selectedBillingDay: Int?,
    selectedBillingDate: LocalDate?,
    onSelectPlan: (Long) -> Unit,
    onSelectBillingDay: (Int) -> Unit,
    onSelectBillingDate: (LocalDate) -> Unit,
) {
    val spacing = LocalSpacing.current
    val selectedPlan = planOptions.firstOrNull { plan -> plan.servicePlanId == selectedPlanId }

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ServiceBrandIcon(
            serviceName = service.name,
            logoUrl = service.logoUrl,
            serviceCode = service.code,
            modifier = Modifier.size(44.dp),
            fallbackTextStyle = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = service.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }

    Text(
        text = "요금제 선택",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    PlanOptionsRow(
        planOptions = planOptions,
        selectedPlanId = selectedPlanId,
        onSelectPlan = onSelectPlan,
    )

    if (selectedPlan?.isYearly() == true) {
        YearlyBillingDatePickerSection(
            selectedBillingDate = selectedBillingDate,
            onSelectBillingDate = onSelectBillingDate,
        )
    } else {
        BillingDayPickerSection(
            selectedBillingDay = selectedBillingDay,
            onSelectBillingDay = onSelectBillingDay,
        )
    }
}

@Composable
private fun BundleConfigurationStep(
    bundle: DashboardBundleItem,
    selectedBillingDay: Int?,
    selectedBillingDate: LocalDate?,
    onSelectBillingDay: (Int) -> Unit,
    onSelectBillingDate: (LocalDate) -> Unit,
) {
    val spacing = LocalSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                Text(
                    text = bundle.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (bundle.planName != bundle.name) {
                    Text(
                        text = bundle.planName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BundleIncludedServicesHero(
                    services = bundle.includedServices,
                    logoSize = 44.dp,
                    minHeight = 124.dp,
                )
                Text(
                    text = "포함서비스 : ${bundle.includedServiceNames()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(spacing.space1),
                    ) {
                        Text(
                            text = "고정 월 금액",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        bundle.originalPrice?.takeIf { originalPrice -> originalPrice > bundle.monthlyPrice }?.let { originalPrice ->
                            Text(
                                text = "정가 ${formatWon(originalPrice)} 대비 할인 상품",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = formatWon(bundle.monthlyPrice),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (bundle.isYearly()) {
            YearlyBillingDatePickerSection(
                selectedBillingDate = selectedBillingDate,
                onSelectBillingDate = onSelectBillingDate,
                title = "결제일을 선택해 주세요",
            )
        } else {
            BillingDayPickerSection(
                selectedBillingDay = selectedBillingDay,
                onSelectBillingDay = onSelectBillingDay,
                title = "결제일을 선택해 주세요",
                showPickerGuide = false,
            )
        }
    }
}

@Composable
private fun BundleIncludedServicesHero(
    services: List<DashboardServiceItem>,
    logoSize: Dp,
    minHeight: Dp,
) {
    val brandVisuals = remember(services) { services.toBundleBrandVisuals() }
    val gradientColors = brandVisuals.resolveBrandGradientColors(
        defaultStart = MaterialTheme.colorScheme.surfaceVariant,
        defaultEnd = MaterialTheme.colorScheme.surface,
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .background(brush = Brush.linearGradient(colors = gradientColors)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.16f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.10f),
                            ),
                        ),
                    ),
            )

            BundleBrandLogoCluster(
                services = brandVisuals,
                modifier = Modifier.align(Alignment.Center),
                logoSize = logoSize,
                overlap = 10.dp,
            )
        }
    }
}

@Composable
private fun IncludedServiceRow(
    services: List<DashboardServiceItem>,
    iconSize: Dp,
) {
    val spacing = LocalSpacing.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        services.forEach { service ->
            ServiceLogoBadge(
                serviceName = service.name,
                logoUrl = service.logoUrl,
                serviceCode = service.code,
                modifier = Modifier.size(iconSize),
                fallbackTextStyle = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private fun List<DashboardServiceItem>.toBundleBrandVisuals(): List<BundleBrandVisualItem> {
    return map { service ->
        BundleBrandVisualItem(
            name = service.name,
            code = service.code,
            logoUrl = service.logoUrl,
        )
    }
}

@Composable
fun ServiceSelectionBottomSheet(
    services: List<DashboardServiceItem>,
    onDismiss: () -> Unit,
    onSelectService: (Long) -> Unit,
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        title = "서비스를 선택해주세요",
    ) {
        ServiceSelectionBody(
            services = services,
            onSelectService = onSelectService,
        )
    }
}

@Composable
fun PlanBillingBottomSheet(
    selectedService: DashboardServiceItem,
    planOptions: List<DashboardPlanItem>,
    selectedPlanId: Long?,
    selectedBillingDay: Int?,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSelectPlan: (Long) -> Unit,
    onSelectBillingDay: (Int) -> Unit,
    onSubmit: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val selectedPlan = planOptions.firstOrNull { it.servicePlanId == selectedPlanId }
    val isReadOnlyYearlyPlan = isEditing && selectedPlan?.isYearly() == true

    LaunchedEffect(selectedBillingDay) {
        if (selectedBillingDay == null) {
            onSelectBillingDay(1)
        }
    }

    AppBottomSheet(
        onDismiss = onDismiss,
        title = if (isEditing) "구독 수정하기" else "요금제와 결제일을 선택해주세요",
        footerContent = {
            AppButton(
                text = if (isEditing) "수정 완료" else "완료",
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Primary,
                size = ButtonSize.Large,
                enabled = selectedPlanId != null && selectedBillingDay != null,
            )
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ServiceLogoBadge(
                serviceName = selectedService.name,
                logoUrl = selectedService.logoUrl,
                modifier = Modifier.size(44.dp),
            )
            Text(
                text = selectedService.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Text(
            text = "요금제 선택",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        PlanOptionsRow(
            planOptions = planOptions,
            selectedPlanId = selectedPlanId,
            onSelectPlan = onSelectPlan,
        )

        if (isReadOnlyYearlyPlan) {
            YearlyBillingInfoCard()
        } else {
            BillingDayPickerSection(
                selectedBillingDay = selectedBillingDay,
                onSelectBillingDay = onSelectBillingDay,
            )
        }
    }
}

@Composable
fun SubscriptionDetailBottomSheet(
    detail: SubscriptionDetailUiModel?,
    isLoading: Boolean,
    errorMessage: String?,
    onRetryLoad: () -> Unit,
    onDismiss: () -> Unit,
    onOpenPromotionDetail: (Long) -> Unit,
    onOpenCancelGuide: (Long) -> Unit,
    onEditSubscription: (Long) -> Unit,
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        title = null,
        scrollable = false,
    ) {
        when {
            isLoading -> {
                AppLoadingState(
                    title = "구독 정보를 불러오는 중이에요.",
                    description = "잠시만 기다려주세요.",
                )
            }

            errorMessage != null -> {
                AppErrorState(
                    title = "구독 정보를 불러오지 못했어요.",
                    description = errorMessage,
                    onRetryClick = onRetryLoad,
                )
            }

            detail == null -> {
                AppErrorState(
                    title = "구독 정보를 찾지 못했어요.",
                    description = "다시 시도해주세요.",
                    onRetryClick = onRetryLoad,
                )
            }

            else -> {
                val spacing = LocalSpacing.current
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.space4),
                ) {
                    item {
                        ServiceHeader(detail = detail)
                    }
                    item {
                        RecommendationSection(
                            recommendations = detail.recommendations,
                            onOpenPromotionDetail = onOpenPromotionDetail,
                        )
                    }
                    item {
                        UsageFlowSection(
                            nextBillingDate = detail.nextBillingDate,
                            usageDaily = detail.usageDaily,
                            isLoading = detail.isUsageDailyLoading,
                            errorMessage = detail.usageDailyErrorMessage,
                        )
                    }
                    item {
                        AppButton(
                            text = "구독 수정하기",
                            onClick = { onEditSubscription(detail.subscriptionId) },
                            modifier = Modifier.fillMaxWidth(),
                            variant = ButtonVariant.Secondary,
                            size = ButtonSize.Medium,
                        )
                    }
                    item {
                        AppButton(
                            text = "해지 가이드 보기",
                            onClick = { onOpenCancelGuide(detail.subscriptionId) },
                            modifier = Modifier.fillMaxWidth(),
                            variant = ButtonVariant.Secondary,
                            size = ButtonSize.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanOptionsRow(
    planOptions: List<DashboardPlanItem>,
    selectedPlanId: Long?,
    onSelectPlan: (Long) -> Unit,
) {
    val spacing = LocalSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        planOptions.forEach { plan ->
            val selected = plan.servicePlanId == selectedPlanId
            Surface(
                modifier = Modifier
                    .widthIn(min = 152.dp)
                    .clip(MaterialTheme.shapes.large)
                    .clickable { onSelectPlan(plan.servicePlanId) },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                ),
            ) {
                Column(
                    modifier = Modifier.padding(all = spacing.space4),
                    verticalArrangement = Arrangement.spacedBy(spacing.space3),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlanOptionPill(text = plan.billingCycleLabel())
                        if (selected) {
                            PlanOptionPill(
                                text = "선택됨",
                                emphasized = true,
                            )
                        }
                    }
                    Text(
                        text = plan.planName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = plan.priceSummaryLabel(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = plan.priceAmountLabel(),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearlyBillingDatePickerSection(
    selectedBillingDate: LocalDate?,
    onSelectBillingDate: (LocalDate) -> Unit,
    title: String = "결제일 선택",
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val todayEpochMillis = remember(today) { today.toEpochMillisAtStartOfDay() }
    val selectableDates = remember(todayEpochMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= todayEpochMillis
            }
        }
    }
    val baseColorScheme = MaterialTheme.colorScheme
    val datePickerDayTextColor = remember(baseColorScheme.onSurface) {
        baseColorScheme.onSurface.copy(alpha = 0.76f)
    }
    val datePickerColorScheme = remember(baseColorScheme, datePickerDayTextColor) {
        baseColorScheme.copy(
            surface = Color.White,
            onSurface = datePickerDayTextColor,
            surfaceVariant = Color.White,
            onSurfaceVariant = datePickerDayTextColor,
            surfaceBright = Color.White,
            surfaceDim = Color.White,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color.White,
            surfaceContainer = Color.White,
            surfaceContainerHigh = Color.White,
            surfaceContainerHighest = Color.White,
        )
    }
    val koreanConfiguration = remember(context) {
        Configuration(context.resources.configuration).apply {
            setLocale(Locale.KOREA)
            setLayoutDirection(Locale.KOREA)
        }
    }
    val koreanContext = remember(context, koreanConfiguration) {
        context.createConfigurationContext(koreanConfiguration)
    }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        CompositionLocalProvider(
            LocalContext provides koreanContext,
            LocalConfiguration provides koreanConfiguration,
        ) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedBillingDate?.toEpochMillisAtStartOfDay(),
                selectableDates = selectableDates,
            )

            MaterialTheme(colorScheme = datePickerColorScheme) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val pickedDate = datePickerState.selectedDateMillis?.toLocalDateAtStartOfDay()
                                    ?: return@TextButton
                                onSelectBillingDate(pickedDate)
                                showDatePicker = false
                            },
                        ) {
                            Text(text = "확인")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text(text = "취소")
                        }
                    },
                ) {
                    Surface(
                        color = Color.White,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        DatePicker(
                            state = datePickerState,
                            title = {
                                Text(
                                    text = "날짜 선택",
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = baseColorScheme.onSurfaceVariant,
                                )
                            },
                            headline = {
                                Text(
                                    text = datePickerState.selectedDateMillis
                                        ?.toLocalDateAtStartOfDay()
                                        ?.toDatePickerHeadline()
                                        ?: "결제일을 선택해 주세요",
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = baseColorScheme.onSurface,
                                )
                            },
                            showModeToggle = false,
                        )
                    }
                }
            }
        }
    }

    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.space4, vertical = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "선택된 결제일",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Text(
                        text = "매년 자동 결제",
                        modifier = Modifier.padding(horizontal = spacing.space2, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = selectedBillingDate?.toYearlyBillingLabel() ?: "달력에서 결제일을 선택해 주세요",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (selectedBillingDate != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = "선택한 날짜를 다음 연간 결제일로 저장합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    AppButton(
        text = if (selectedBillingDate == null) "달력으로 결제일 선택" else "달력에서 다시 선택",
        onClick = { showDatePicker = true },
        modifier = Modifier.fillMaxWidth(),
        variant = ButtonVariant.Secondary,
        size = ButtonSize.Medium,
    )
}

@Composable
private fun BillingDayPickerSection(
    selectedBillingDay: Int?,
    onSelectBillingDay: (Int) -> Unit,
    title: String = "결제일 선택",
    showPickerGuide: Boolean = true,
) {
    val spacing = LocalSpacing.current
    val resolvedBillingDay = (selectedBillingDay ?: 1).coerceIn(BILLING_DAY_RANGE.first, BILLING_DAY_RANGE.last)

    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.space4, vertical = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "선택된 결제일",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    shape = PillShape,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Text(
                        text = "매월 자동 결제",
                        modifier = Modifier.padding(horizontal = spacing.space2, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "매월",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${resolvedBillingDay}일",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = "선택한 날짜를 기준으로 다음 결제일이 저장됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.space4, vertical = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space3),
        ) {
            if (showPickerGuide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "1일에서 31일 사이에서 골라주세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        shape = PillShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = "탭 또는 스크롤",
                            modifier = Modifier.padding(horizontal = spacing.space2, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            BillingDayWheelPicker(
                selectedBillingDay = resolvedBillingDay,
                onSelectBillingDay = onSelectBillingDay,
                itemHeight = 52.dp,
            )
        }
    }
}

@Composable
private fun BillingDayWheelPicker(
    selectedBillingDay: Int,
    onSelectBillingDay: (Int) -> Unit,
    itemHeight: Dp,
) {
    val spacing = LocalSpacing.current
    val days = remember { BILLING_DAY_RANGE.toList() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedBillingDay - 1)
    val latestSelectedBillingDay = rememberUpdatedState(selectedBillingDay)
    val itemHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { itemHeight.roundToPx() }

    LaunchedEffect(selectedBillingDay) {
        val targetIndex = (selectedBillingDay - 1).coerceIn(days.indices)
        if (listState.firstVisibleItemIndex != targetIndex || listState.firstVisibleItemScrollOffset != 0) {
            listState.scrollToItem(targetIndex)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collectLatest { isScrolling ->
                if (isScrolling) return@collectLatest

                val targetIndex = settledBillingDayIndex(
                    firstVisibleItemIndex = listState.firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                    itemHeightPx = itemHeightPx,
                    maxIndex = days.lastIndex,
                )
                if (listState.firstVisibleItemIndex != targetIndex || listState.firstVisibleItemScrollOffset != 0) {
                    listState.scrollToItem(targetIndex)
                }

                val targetDay = days[targetIndex]
                if (targetDay != latestSelectedBillingDay.value) {
                    onSelectBillingDay(targetDay)
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight * WHEEL_VISIBLE_ITEM_COUNT),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = spacing.space2)
                .height(itemHeight),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
        ) {}

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                vertical = itemHeight * ((WHEEL_VISIBLE_ITEM_COUNT - 1) / 2),
            ),
        ) {
            items(days) { day ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clip(MaterialTheme.shapes.large)
                        .clickable {
                            if (day != selectedBillingDay) {
                                onSelectBillingDay(day)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${day}일",
                        style = if (day == selectedBillingDay) {
                            MaterialTheme.typography.titleLarge
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        color = if (day == selectedBillingDay) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(itemHeight * 1.5f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(itemHeight * 1.5f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun YearlyBillingInfoCard() {
    val spacing = LocalSpacing.current

    Text(
        text = "결제일 정보",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.space4, vertical = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Text(
                text = "연간 요금제는 결제일을 직접 수정하지 않아요.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "현재 등록된 다음 결제일 기준을 유지합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ServiceHeader(detail: SubscriptionDetailUiModel) {
    val spacing = LocalSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ServiceLogoBadge(
                serviceName = detail.serviceName,
                logoUrl = detail.logoUrl,
                modifier = Modifier.size(48.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "${detail.serviceName} (${detail.planName})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = detail.billingCycleLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = detail.priceLabel(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = detail.nextBillingDateLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UsageFlowSection(
    nextBillingDate: String?,
    usageDaily: List<SubscriptionDetailUsagePoint>,
    isLoading: Boolean,
    errorMessage: String?,
) {
    val spacing = LocalSpacing.current

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            if (usageDaily.isEmpty()) {
                when {
                    isLoading -> {
                        Text(
                            text = "사용 기록을 불러오는 중이에요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    errorMessage != null -> {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> {
                        Text(
                            text = "아직 사용 기록이 없어요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                SubscriptionDetailHeatmap(
                    points = usageDaily,
                    nextBillingDate = nextBillingDate,
                )
                Text(
                    text = "테두리로 표시된 날은 결제일이에요",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SubscriptionDetailHeatmap(
    points: List<SubscriptionDetailUsagePoint>,
    nextBillingDate: String?,
) {
    val cellSize = 28.dp
    val cellSpacing = 8.dp
    val tooltipWidth = 104.dp
    val tooltipHeight = 54.dp
    val tooltipGap = 8.dp
    val monthSections = remember(points) { points.toDetailMonthSections() }
    val billingDayOfMonth = remember(nextBillingDate) {
        nextBillingDate
            ?.let { raw -> runCatching { LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull() }
            ?.dayOfMonth
    }
    var selectedMonthIndex by remember(monthSections) {
        mutableStateOf((monthSections.lastIndex).coerceAtLeast(0))
    }
    val currentSection = monthSections.getOrNull(selectedMonthIndex)
    var selectedIndex by remember(currentSection?.yearMonth) { mutableStateOf<Int?>(null) }
    val selectedPoint = selectedIndex?.let { index -> currentSection?.points?.getOrNull(index) }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != null) {
            delay(2000)
            selectedIndex = null
        }
    }

    if (currentSection == null) {
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (selectedMonthIndex > 0) "<" else "",
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(enabled = selectedMonthIndex > 0) {
                        selectedMonthIndex -= 1
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                color = if (selectedMonthIndex > 0) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    Color.Transparent
                },
            )
            Text(
                text = "${currentSection.yearMonth.monthValue}월 사용 기록",
                modifier = Modifier.padding(horizontal = 14.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (selectedMonthIndex < monthSections.lastIndex) ">" else "",
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(enabled = selectedMonthIndex < monthSections.lastIndex) {
                        selectedMonthIndex += 1
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                color = if (selectedMonthIndex < monthSections.lastIndex) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    Color.Transparent
                },
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val containerMaxWidth = maxWidth
            val maxTooltipOffsetX = (containerMaxWidth - tooltipWidth).coerceAtLeast(0.dp)

            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(cellSpacing),
                ) {
                    currentSection.points.chunked(7).forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                cellSpacing,
                                Alignment.CenterHorizontally,
                            ),
                        ) {
                            row.forEachIndexed { columnIndex, point ->
                                val absoluteIndex = rowIndex * 7 + columnIndex
                                val pointDate = LocalDate.parse(point.usageDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(point.resolveDetailUsageColor())
                                        .then(
                                            if (billingDayOfMonth != null && pointDate.dayOfMonth == billingDayOfMonth) {
                                                Modifier.border(
                                                    width = 2.dp,
                                                    color = Color.Black.copy(alpha = 0.72f),
                                                    shape = MaterialTheme.shapes.small,
                                                )
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .clickable { selectedIndex = absoluteIndex },
                                ) {
                                }
                            }
                        }
                    }
                }

                if (selectedPoint != null && selectedIndex != null) {
                    val rowIndex = selectedIndex!! / 7
                    val columnIndex = selectedIndex!! % 7
                    val visibleRowSize = currentSection.points.chunked(7)
                        .getOrNull(rowIndex)
                        ?.size
                        ?: 0
                    val rowWidth = cellSize * visibleRowSize +
                        cellSpacing * (visibleRowSize - 1).coerceAtLeast(0)
                    val startOffsetX = ((containerMaxWidth - rowWidth) / 2).coerceAtLeast(0.dp)
                    val rawOffsetX = startOffsetX + (cellSize + cellSpacing) * columnIndex
                    val centeredOffsetX = rawOffsetX - (tooltipWidth - cellSize) / 2
                    val offsetX = centeredOffsetX.coerceIn(0.dp, maxTooltipOffsetX)
                    val offsetY = (cellSize + cellSpacing) * rowIndex - tooltipHeight - tooltipGap

                    Surface(
                        modifier = Modifier.offset {
                            IntOffset(
                                x = offsetX.roundToPx(),
                                y = offsetY.roundToPx(),
                            )
                        },
                        shape = MaterialTheme.shapes.medium,
                        color = Color(0xFF2C313A),
                        shadowElevation = 6.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = selectedPoint.toDetailTooltipDateLabel(),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "${selectedPoint.usedMinutes}분 시청",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.86f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionDetailUsagePoint.resolveDetailUsageColor(): Color {
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    return when {
        usedMinutes <= 0 -> surface
        usedMinutes <= 20 -> lerp(surface, primary, 0.24f)
        usedMinutes <= 60 -> lerp(surface, primary, 0.52f)
        else -> primary
    }
}

private fun List<SubscriptionDetailUsagePoint>.toDetailMonthSections(): List<DetailMonthSectionUiModel> {
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    return sortedBy { point -> point.usageDate }
        .groupBy { point -> YearMonth.from(LocalDate.parse(point.usageDate, formatter)) }
        .entries
        .sortedBy { entry -> entry.key }
        .map { (yearMonth, groupedPoints) ->
            DetailMonthSectionUiModel(
                yearMonth = yearMonth,
                points = groupedPoints.sortedBy { point -> point.usageDate },
            )
        }
}

private fun SubscriptionDetailUsagePoint.toDetailTooltipDateLabel(): String {
    val date = LocalDate.parse(usageDate, DateTimeFormatter.ISO_LOCAL_DATE)
    return "${date.monthValue}월 ${date.dayOfMonth}일"
}

private data class DetailMonthSectionUiModel(
    val yearMonth: YearMonth,
    val points: List<SubscriptionDetailUsagePoint>,
)

@Composable
private fun RecommendationSection(
    recommendations: List<RecommendationUiItem>,
    onOpenPromotionDetail: (Long) -> Unit,
) {
    val recommendation = recommendations.firstOrNull()

    if (recommendation == null) {
        AppCard {
            Text(
                text = "지금 확인할 수 있는 추천 혜택이 없어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    AppCard(onClick = { onOpenPromotionDetail(recommendation.promotionId) }) {
        Column(verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.space4)) {
            Text(
                text = "지금 확인해보는 혜택",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            RecommendationHeroImage(
                recommendation = recommendation,
            )

            Column(verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.space2)) {
                Text(
                    text = recommendation.headline,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                recommendation.primaryReason?.let { reason ->
                    RecommendationPrimaryReasonChip(reason = reason)
                }

                recommendation.summary?.takeIf { summary -> summary.isNotBlank() }?.let { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            RecommendationPriceComparison(recommendation = recommendation)
        }
    }
}

@Composable
private fun RecommendationHeroImage(
    recommendation: RecommendationUiItem,
) {
    val brandVisuals = remember(recommendation.services) {
        recommendation.services.toRecommendationBrandVisuals()
    }
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
        if (!recommendation.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = recommendation.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (recommendation.promotionType == PromotionType.Bundle) 0.24f else 0.94f,
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
            RecommendationServiceLogoRow(
                services = recommendation.services,
                modifier = Modifier.align(Alignment.Center),
            )

            recommendation.savingsBadgeText?.let { badgeText ->
                RecommendationSavingsBadge(
                    text = badgeText,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}

@Composable
private fun RecommendationPrimaryReasonChip(
    reason: String,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary,
    ) {
        Text(
            text = reason,
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
private fun RecommendationServiceLogoRow(
    services: List<RecommendationServiceLogoUiItem>,
    modifier: Modifier = Modifier,
) {
    BundleBrandLogoCluster(
        services = services.toRecommendationBrandVisuals(),
        modifier = modifier,
        logoSize = 38.dp,
        overlap = 8.dp,
    )
}

private fun List<RecommendationServiceLogoUiItem>.toRecommendationBrandVisuals(): List<BundleBrandVisualItem> {
    return map { service ->
        BundleBrandVisualItem(
            name = service.label,
            code = service.code,
            logoUrl = service.logoUrl,
        )
    }
}

@Composable
private fun RecommendationSavingsBadge(
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
private fun RecommendationPriceComparison(
    recommendation: RecommendationUiItem,
) {
    val originalPriceLabel = recommendation.originalPriceLabel
    val discountPriceLabel = recommendation.discountPriceLabel
    val hasPriceComparison = !originalPriceLabel.isNullOrBlank() && !discountPriceLabel.isNullOrBlank()

    if (!hasPriceComparison) {
        Text(
            text = "눌러서 혜택 상세를 확인해보세요.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = originalPriceLabel.orEmpty(),
            style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.LineThrough),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = discountPriceLabel.orEmpty(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SpacerCardPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier)
}

private fun formatWon(amount: Int): String {
    return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원"
}

private fun DashboardBundleItem.includedServiceNames(): String {
    return includedServices
        .map { service -> service.name }
        .filter { name -> name.isNotBlank() }
        .distinct()
        .joinToString(separator = ", ")
        .ifBlank { "포함 서비스 정보 없음" }
}

private fun DashboardBundleItem.isYearly(): Boolean = billingCycle.equals("YEARLY", ignoreCase = true)

private fun DashboardPlanItem.isYearly(): Boolean = billingCycle.equals("YEARLY", ignoreCase = true)

private fun DashboardPlanItem.billingCycleLabel(): String = if (isYearly()) "연간" else "월간"

private fun DashboardPlanItem.priceSummaryLabel(): String {
    return if (isYearly()) {
        "연간 요금제 (월 환산)"
    } else {
        "월 결제 금액"
    }
}

private fun DashboardPlanItem.priceAmountLabel(): String = formatWon(monthlyPrice)

private fun DashboardPlanItem.priceLabel(): String {
    return if (isYearly()) {
        "월 환산 ${formatWon(monthlyPrice)}"
    } else {
        "월 ${formatWon(monthlyPrice)}"
    }
}

private fun SubscriptionDetailUiModel.billingCycleLabel(): String {
    return if (billingCycle.equals("YEARLY", ignoreCase = true)) "연간 요금제" else "월간 요금제"
}

private fun SubscriptionDetailUiModel.priceLabel(): String {
    return if (billingCycle.equals("YEARLY", ignoreCase = true)) {
        "월 환산 ${formatWon(monthlyPrice)}"
    } else {
        formatWon(monthlyPrice)
    }
}

private fun RecommendationUiItem.toSavingLabel(amount: Int): String {
    val periodLabel = when (billingCycle) {
        "YEARLY" -> "연 최대"
        else -> "월 최대"
    }
    return "$periodLabel ${formatWon(amount)} 할인 가능"
}

private fun settledBillingDayIndex(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    itemHeightPx: Int,
    maxIndex: Int,
): Int {
    val shouldMoveForward = firstVisibleItemScrollOffset >= itemHeightPx / 2
    val rawIndex = firstVisibleItemIndex + if (shouldMoveForward) 1 else 0
    return rawIndex.coerceIn(0, maxIndex)
}

private fun LocalDate.toYearlyBillingLabel(): String = "매년 ${monthValue}월 ${dayOfMonth}일 결제"

private fun LocalDate.toDatePickerHeadline(): String {
    return format(DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREA))
}

private fun LocalDate.toEpochMillisAtStartOfDay(): Long {
    return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun Long.toLocalDateAtStartOfDay(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
}

private val BILLING_DAY_RANGE = 1..31
private const val WHEEL_VISIBLE_ITEM_COUNT = 3
