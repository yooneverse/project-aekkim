package com.ssafy.e106.feature.dashboard

import com.ssafy.e106.data.repository.BundleSubscriptionOption
import com.ssafy.e106.data.repository.OttService
import com.ssafy.e106.data.repository.PendingReviewItem
import com.ssafy.e106.data.repository.PromotionDetailData
import com.ssafy.e106.data.repository.PromotionFeedData
import com.ssafy.e106.data.repository.PromotionRecommendationData
import com.ssafy.e106.data.repository.PromotionServiceData
import com.ssafy.e106.data.repository.PromotionType
import com.ssafy.e106.data.repository.ServicePlanBundle
import com.ssafy.e106.data.repository.ServicePlanOption
import com.ssafy.e106.data.repository.SubscriptionDetailData
import com.ssafy.e106.data.repository.SubscriptionOverview
import com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageDailyPointResponse
import com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageReportItemResponse
import com.ssafy.e106.feature.analysis.model.TrackedSubscriptionPackages
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

internal fun SubscriptionOverview.toSubscriptionCardItem(): SubscriptionCardItem {
    return SubscriptionCardItem(
        subscriptionId = subscriptionId,
        serviceId = serviceId,
        servicePlanId = servicePlanId,
        subscriptionType = subscriptionType,
        bundleCode = bundleCode,
        serviceName = serviceName,
        planName = planName,
        billingCycle = billingCycle.inferDashboardBillingCycle(planName = planName),
        logoUrl = logoUrl,
        monthlyPrice = monthlyPrice,
        nextBillingDate = nextBillingDate,
        expectedSavingAmount = expectedSavingAmount,
        savingLabel = savingLabel,
        coveredServices = coveredServices.map { coveredService ->
            DashboardServiceItem(
                serviceId = coveredService.serviceId,
                code = coveredService.serviceCode.orEmpty(),
                name = coveredService.serviceName,
                logoUrl = coveredService.logoUrl,
            )
        },
    )
}

internal fun PendingReviewItem.toPendingReviewUiItem(): PendingReviewUiItem {
    return PendingReviewUiItem(
        reviewId = reviewId,
        merchantName = merchantName,
        suggestedServiceName = suggestedServiceName,
        monthlyAmount = monthlyAmount,
        billedAtLabel = billedAtLabel,
    )
}

internal fun OttService.toDashboardServiceItem(): DashboardServiceItem {
    val trackedService = TrackedSubscriptionPackages.findServiceByCode(code)
        ?: TrackedSubscriptionPackages.findServiceByName(name)
    return DashboardServiceItem(
        serviceId = serviceId,
        code = code,
        name = name,
        logoUrl = logoUrl,
        aliases = buildSet {
            add(code)
            add(name)
            addAll(trackedService?.aliases.orEmpty())
        },
    )
}

internal fun List<OttService>.toDashboardServiceItems(): List<DashboardServiceItem> {
    return map { service -> service.toDashboardServiceItem() }
        .distinctBy { service -> service.serviceId }
        .sortedWith(dashboardServiceComparator)
}

internal fun ServicePlanBundle.toDashboardServiceItem(): DashboardServiceItem {
    val trackedService = TrackedSubscriptionPackages.findServiceByCode(code)
        ?: TrackedSubscriptionPackages.findServiceByName(name)
    return DashboardServiceItem(
        serviceId = serviceId,
        code = code,
        name = name,
        logoUrl = logoUrl,
        aliases = buildSet {
            add(code)
            add(name)
            addAll(trackedService?.aliases.orEmpty())
        },
    )
}

internal fun ServicePlanOption.toDashboardPlanItem(): DashboardPlanItem {
    return DashboardPlanItem(
        servicePlanId = servicePlanId,
        planName = planName,
        billingCycle = billingCycle,
        monthlyPrice = monthlyPrice,
    )
}

internal fun BundleSubscriptionOption.toDashboardBundleItem(): DashboardBundleItem {
    return DashboardBundleItem(
        code = code,
        name = name,
        planName = planName,
        billingCycle = billingCycle,
        monthlyPrice = monthlyPrice,
        originalPrice = originalPrice,
        logoUrl = logoUrl,
        includedServices = includedServices.map { service -> service.toDashboardServiceItem() },
    )
}

internal fun SubscriptionDetailData.toSubscriptionDetailUiModel(): SubscriptionDetailUiModel {
    return SubscriptionDetailUiModel(
        subscriptionId = subscriptionId,
        serviceName = serviceName,
        logoUrl = logoUrl,
        planName = planName,
        billingCycle = billingCycle,
        monthlyPrice = monthlyPrice,
        nextBillingDate = nextBillingDate,
        nextBillingDateLabel = billingCycle.toNextBillingDateLabel(nextBillingDate),
        recommendations = recommendations.map { recommendation ->
            RecommendationUiItem(
                promotionId = recommendation.promotionId,
                headline = recommendation.headline,
                monthlySavingAmount = recommendation.monthlySavingAmount,
                billingCycle = recommendation.billingCycle,
                promotionType = runCatching { PromotionType.valueOf(recommendation.promotionType) }.getOrNull(),
                summary = recommendation.title.takeIf { title -> title.isNotBlank() && title != recommendation.headline },
                savingsBadgeText = recommendation.monthlySavingAmount
                    ?.takeIf { amount -> amount > 0 }
                    ?.let { amount -> "월 ${formatWon(amount)} 절감" },
                imageUrl = recommendation.imageUrl,
                services = recommendation.services.map(PromotionServiceData::toRecommendationServiceLogoUiItem),
            )
        },
        cancelGuideUrl = cancelGuideUrl,
        customerServicePhone = customerServicePhone,
        contactEmail = contactEmail,
    )
}

internal fun List<SubscriptionUsageDailyPointResponse>.toSubscriptionDetailUsagePoints(): List<SubscriptionDetailUsagePoint> {
    return map { point ->
        SubscriptionDetailUsagePoint(
            usageDate = point.usageDate,
            usedMinutes = point.totalUsedMinutes,
        )
    }
}

internal fun RecommendationUiItem.enrich(detail: PromotionDetailData): RecommendationUiItem {
    val monthlySavedAmount = detail.price.monthlySavedAmount
    return copy(
        promotionType = detail.promotionType,
        summary = detail.summary,
        savingsBadgeText = if (monthlySavedAmount > 0) {
            "월 ${formatWon(monthlySavedAmount)} 절감"
        } else {
            detail.promotionType.toRecommendationBadgeText()
        },
        primaryReason = when {
            monthlySavedAmount > 0 -> "절약 효과가 큰 편이에요"
            detail.promotionType == PromotionType.CardBenefit -> "지금 쓰는 서비스와 바로 연결돼요"
            detail.promotionType == PromotionType.Bundle -> "여러 서비스를 함께 보는 조합이에요"
            else -> "지금 확인해볼 만한 혜택이에요"
        },
        imageUrl = detail.imageUrl,
        services = detail.services.map(PromotionServiceData::toRecommendationServiceLogoUiItem),
        originalPriceLabel = detail.price.originalPrice?.let(::formatWon),
        discountPriceLabel = detail.price.discountPrice?.let(::formatWon),
    )
}

internal fun SubscriptionCardItem.withUsageInsight(
    reportItem: SubscriptionUsageReportItemResponse?,
): SubscriptionCardItem {
    return copy(
        nudgeMessage = reportItem?.nudgeMessage,
    )
}

internal fun List<SubscriptionCardItem>.totalExpectedSavingAmount(): Int {
    return sumOf { item -> item.expectedSavingAmount?.coerceAtLeast(0) ?: 0 }
}

internal fun List<SubscriptionCardItem>.calculatePromotionFeedExpectedSavingAmount(
    promotionFeed: PromotionFeedData,
): Int {
    if (isEmpty()) return 0

    val subscribedServiceIds = asSequence()
        .flatMap { item ->
            buildList {
                if (item.serviceId > 0L) {
                    add(item.serviceId)
                }
                item.coveredServices.forEach { service ->
                    if (service.serviceId > 0L) {
                        add(service.serviceId)
                    }
                }
            }.asSequence()
        }
        .distinct()
        .toList()
    if (subscribedServiceIds.isEmpty()) return 0

    val serviceBitIndex = subscribedServiceIds.withIndex().associate { (index, serviceId) ->
        serviceId to index
    }
    val candidates = promotionFeed.categories
        .asSequence()
        .flatMap { category ->
            sequenceOf(category.bundles, category.promotions, category.cardBenefits)
                .flatMap { recommendations -> recommendations.asSequence() }
        }
        .mapNotNull { recommendation ->
            recommendation.toSavingCandidate(serviceBitIndex)
        }
        .groupBy(SavingCandidate::promotionId)
        .values
        .mapNotNull { duplicates ->
            duplicates.maxByOrNull(SavingCandidate::monthlySavedAmount)
        }
        .toList()
    if (candidates.isEmpty()) return 0

    val bestSavingByMask = mutableMapOf(0L to 0)
    candidates.forEach { candidate ->
        val snapshot = bestSavingByMask.toMap()
        snapshot.forEach { (mask, accumulatedSaving) ->
            if ((mask and candidate.serviceMask) != 0L) return@forEach

            val nextMask = mask or candidate.serviceMask
            val nextSaving = accumulatedSaving + candidate.monthlySavedAmount
            val currentBestSaving = bestSavingByMask[nextMask] ?: 0
            if (nextSaving > currentBestSaving) {
                bestSavingByMask[nextMask] = nextSaving
            }
        }
    }

    return bestSavingByMask.values.maxOrNull() ?: 0
}

private fun PromotionRecommendationData.toSavingCandidate(
    serviceBitIndex: Map<Long, Int>,
): SavingCandidate? {
    val monthlySavedAmount = price.monthlySavedAmount.coerceAtLeast(0)
    if (monthlySavedAmount <= 0) return null

    val serviceMask = services
        .asSequence()
        .mapNotNull { service -> serviceBitIndex[service.serviceId] }
        .distinct()
        .fold(0L) { mask, index -> mask or (1L shl index) }
    if (serviceMask == 0L) return null

    return SavingCandidate(
        promotionId = promotionId,
        serviceMask = serviceMask,
        monthlySavedAmount = monthlySavedAmount,
    )
}

private data class SavingCandidate(
    val promotionId: Long,
    val serviceMask: Long,
    val monthlySavedAmount: Int,
)

// Dashboard list API does not expose billingCycle yet, so keep a plan-name fallback in FE.
private fun String?.inferDashboardBillingCycle(planName: String?): String? {
    if (!this.isNullOrBlank()) return this

    val normalizedPlanName = planName
        ?.lowercase(Locale.ROOT)
        ?.replace(" ", "")
        ?: return null

    return when {
        normalizedPlanName.contains("연간") ||
            normalizedPlanName.contains("1년") ||
            normalizedPlanName.contains("12개월") ||
            normalizedPlanName.contains("annual") ||
            normalizedPlanName.contains("yearly") ||
            normalizedPlanName.contains("12month") -> "YEARLY"
        normalizedPlanName.contains("월간") ||
            normalizedPlanName.contains("매월") ||
            normalizedPlanName.contains("monthly") -> "MONTHLY"
        else -> null
    }
}

private fun DashboardServiceItem.brandOrderIndex(): Int {
    val trackedCode = TrackedSubscriptionPackages.findServiceByCode(code)?.code
        ?: TrackedSubscriptionPackages.findServiceByName(name)?.code
        ?: return Int.MAX_VALUE

    return dashboardServiceOrder.indexOf(trackedCode).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE
}

private val dashboardServiceOrder = listOf(
    "NETFLIX",
    "TVING",
    "COUPANG_PLAY",
    "WAVVE",
    "WATCHA",
    "DISNEY_PLUS",
    "MELON",
    "BUGS",
    "YOUTUBE_MUSIC",
    "SPOTIFY",
    "CHATGPT",
    "GEMINI",
    "CLAUDE",
)

private fun PromotionServiceData.toRecommendationServiceLogoUiItem(): RecommendationServiceLogoUiItem {
    return RecommendationServiceLogoUiItem(
        serviceId = serviceId,
        label = name.ifBlank { code ?: "서비스" },
        code = code,
        logoUrl = logoUrl,
    )
}

private fun PromotionType.toRecommendationBadgeText(): String {
    return when (this) {
        PromotionType.Bundle -> "번들 혜택"
        PromotionType.CardBenefit -> "카드 혜택"
        PromotionType.Promo -> "프로모션"
    }
}

private fun formatWon(amount: Int): String {
    return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원"
}

private fun String?.toNextBillingDateLabel(nextBillingDate: String?): String {
    val parsedDate = nextBillingDate
        ?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }
        ?: return "다음 결제일 확인 필요"

    return if (this.equals("YEARLY", ignoreCase = true)) {
        "매년 ${parsedDate.monthValue}월 ${parsedDate.dayOfMonth}일 결제"
    } else {
        "다음 결제일 ${parsedDate.monthValue}월 ${parsedDate.dayOfMonth}일"
    }
}

private val dashboardServiceComparator = compareBy<DashboardServiceItem>(
    { service -> service.brandOrderIndex() },
    { service -> service.name },
)

internal data class DashboardAddSearchResults(
    val services: List<DashboardServiceItem> = emptyList(),
    val bundles: List<DashboardBundleItem> = emptyList(),
)

internal fun filterDashboardAddTargets(
    query: String,
    services: List<DashboardServiceItem>,
    bundles: List<DashboardBundleItem>,
): DashboardAddSearchResults {
    val normalizedQuery = query.normalizeDashboardSearchToken()
    if (normalizedQuery.isBlank()) {
        return DashboardAddSearchResults(
            services = services.sortedWith(dashboardServiceComparator),
            bundles = bundles.sortedWith(
                compareBy<DashboardBundleItem>(
                    { bundle -> bundle.includedServices.none { service -> service.code == "NETFLIX" } },
                    { bundle -> bundle.name },
                    { bundle -> bundle.monthlyPrice },
                ),
            ),
        )
    }

    return DashboardAddSearchResults(
        services = services
            .mapNotNull { service ->
                val score = service.matchScore(normalizedQuery)
                if (score <= 0) null else service to score
            }
            .sortedWith(
                compareByDescending<Pair<DashboardServiceItem, Int>> { (_, score) -> score }
                    .thenBy { (service, _) -> service.brandOrderIndex() }
                    .thenBy { (service, _) -> service.name },
            )
            .map { (service, _) -> service },
        bundles = bundles
            .mapNotNull { bundle ->
                val score = bundle.matchScore(normalizedQuery)
                if (score <= 0) null else bundle to score
            }
            .sortedWith(
                compareByDescending<Pair<DashboardBundleItem, Int>> { (_, score) -> score }
                    .thenBy { (bundle, _) -> bundle.name }
                    .thenBy { (bundle, _) -> bundle.monthlyPrice },
            )
            .map { (bundle, _) -> bundle },
    )
}

private fun DashboardServiceItem.matchScore(normalizedQuery: String): Int {
    return searchTokens().maxOfOrNull { token -> token.matchScore(normalizedQuery) } ?: 0
}

private fun DashboardBundleItem.matchScore(normalizedQuery: String): Int {
    return searchTokens().maxOfOrNull { token -> token.matchScore(normalizedQuery) } ?: 0
}

private fun DashboardServiceItem.searchTokens(): Sequence<String> {
    return sequenceOf(code, name)
        .plus(aliases.asSequence())
}

private fun DashboardBundleItem.searchTokens(): Sequence<String> {
    val serviceTokens = includedServices.asSequence().flatMap { service ->
        sequenceOf(service.code, service.name).plus(service.aliases.asSequence())
    }
    return sequenceOf(code, name, planName)
        .plus(serviceTokens)
}

private fun String.matchScore(normalizedQuery: String): Int {
    val normalizedCandidate = normalizeDashboardSearchToken()
    if (normalizedCandidate.isBlank()) return 0
    return when {
        normalizedCandidate == normalizedQuery -> 500
        normalizedCandidate.startsWith(normalizedQuery) -> 320
        normalizedCandidate.contains(normalizedQuery) -> 180
        else -> 0
    }
}

private fun String.normalizeDashboardSearchToken(): String {
    return trim()
        .lowercase(Locale.ROOT)
        .filterNot { character -> character.isWhitespace() || character == '+' || character == '-' || character == '_' }
}
