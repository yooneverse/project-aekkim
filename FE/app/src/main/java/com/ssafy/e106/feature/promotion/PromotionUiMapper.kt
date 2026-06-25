package com.ssafy.e106.feature.promotion

import com.ssafy.e106.data.repository.PromotionDetailData
import com.ssafy.e106.data.repository.PromotionFeedData
import com.ssafy.e106.data.repository.PromotionPriceData
import com.ssafy.e106.data.repository.PromotionRecommendationCategoryData
import com.ssafy.e106.data.repository.PromotionRecommendationData
import com.ssafy.e106.data.repository.PromotionServiceData
import com.ssafy.e106.data.repository.PromotionType
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun PromotionFeedData.toListScreenState(): PromotionListScreenState {
    val mappedCategories = categories.toCategoryUiModels()

    return if (mappedCategories.isEmpty()) {
        PromotionListScreenState.Empty
    } else {
        PromotionListScreenState.Success(categories = mappedCategories)
    }
}

internal fun PromotionDetailData.toDetailScreenState(): PromotionDetailScreenState {
    val promotion = toUiModel()
    val isExpired = endsAt.toLocalDateTimeOrNull()?.isBefore(LocalDateTime.now()) ?: false

    return when {
        isExpired -> PromotionDetailScreenState.Expired(promotion)
        sourceUrl.isNullOrBlank() -> PromotionDetailScreenState.NoLink(promotion)
        else -> PromotionDetailScreenState.Success(promotion)
    }
}

private fun List<PromotionRecommendationCategoryData>.toCategoryUiModels(): List<PromotionRecommendationCategoryUiModel> {
    val categoriesByKey = associateBy { category -> category.category.uppercase(Locale.ROOT) }

    val mappedCategories = recommendationCategoryOrder.map { categoryKey ->
        val category = categoriesByKey[categoryKey]
        PromotionRecommendationCategoryUiModel(
            categoryKey = categoryKey,
            categoryLabel = categoryKey.toCategoryLabel(),
            bundles = category?.bundles?.map(PromotionRecommendationData::toUiModel).orEmpty(),
            promotions = category?.promotions?.map(PromotionRecommendationData::toUiModel).orEmpty(),
            cardBenefits = category?.cardBenefits?.map(PromotionRecommendationData::toUiModel).orEmpty(),
        )
    }

    return mappedCategories.takeIf { categories -> categories.any(PromotionRecommendationCategoryUiModel::hasAnyItems) }
        .orEmpty()
}

private fun PromotionRecommendationData.toUiModel(): PromotionCardUiModel {
    return PromotionCardUiModel(
        promotionId = promotionId,
        promotionType = promotionType,
        headline = headline,
        summary = summary,
        priceSummary = price.toUiModel(),
        savingsBadgeText = buildSavingsBadgeText(
            promotionType = promotionType,
            price = price,
        ),
        recommendationReasons = recommendationReasons
            .filter { it.isNotBlank() }
            .distinct()
            .take(2),
        conditionDescription = conditionDescription,
        sourceUrl = sourceUrl,
        imageUrl = imageUrl,
        services = services.map(PromotionServiceData::toUiModel),
    )
}

private fun PromotionDetailData.toUiModel(): PromotionDetailUiModel {
    val priceSummary = price.toUiModel()
    val hasNumericSavings = price.monthlySavedAmount > 0

    return PromotionDetailUiModel(
        promotionId = promotionId,
        promotionType = promotionType,
        headline = headline,
        summary = summary,
        priceSummary = priceSummary,
        savingsBadgeText = buildSavingsBadgeText(
            promotionType = promotionType,
            price = price,
        ),
        priceContentType = buildDetailPriceContentType(
            promotionType = promotionType,
            priceSummary = priceSummary,
        ),
        priceSummaryDescription = summary?.takeIf { it.isNotBlank() } ?: buildFallbackSummaryDescription(promotionType),
        monthlySavedDescription = buildMonthlySavedDescription(
            promotionType = promotionType,
            monthlySavedAmount = price.monthlySavedAmount,
        ),
        yearlySavedTitle = if (hasNumericSavings) "연간 절감액" else "혜택 안내",
        yearlySavedLabel = if (hasNumericSavings) formatWon(yearlySavedAmount) else "혜택 확인",
        yearlyBenefitDescription = if (hasNumericSavings) {
            yearlyBenefitDescription
        } else {
            summary?.takeIf { it.isNotBlank() } ?: buildFallbackSummaryDescription(promotionType)
        },
        conditionDescription = conditionDescription,
        detailDescription = detailDescription,
        applySteps = applySteps,
        actionButtonText = "혜택 적용하러 가기",
        sourceUrl = sourceUrl,
        imageUrl = imageUrl,
        services = services.map(PromotionServiceData::toUiModel),
        validityPeriodLabel = buildValidityPeriodLabel(
            startsAt = startsAt,
            endsAt = endsAt,
        ),
    )
}

private fun PromotionPriceData.toUiModel(): PromotionPriceSummaryUiModel {
    return PromotionPriceSummaryUiModel(
        originalPriceLabel = originalPrice?.let(::formatWon),
        discountPriceLabel = discountPrice?.let(::formatWon),
        monthlySavedLabel = formatWon(monthlySavedAmount),
    )
}

private fun PromotionServiceData.toUiModel(): PromotionServiceLogoUiModel {
    return PromotionServiceLogoUiModel(
        serviceId = serviceId,
        label = name.ifBlank { code ?: "서비스" },
        code = code,
        logoUrl = logoUrl,
    )
}

private fun String.toCategoryLabel(): String {
    return when (uppercase(Locale.ROOT)) {
        "OTT" -> "OTT"
        "MUSIC" -> "음악"
        "AI" -> "AI"
        else -> this
    }
}

private fun buildSavingsBadgeText(
    promotionType: PromotionType,
    price: PromotionPriceData,
): String {
    return if (price.monthlySavedAmount > 0) {
        "월 ${formatWon(price.monthlySavedAmount)} 절감"
    } else {
        when (promotionType) {
            PromotionType.CardBenefit -> "카드 혜택"
            PromotionType.Bundle -> "번들 혜택"
            PromotionType.Promo -> "프로모션"
        }
    }
}

private fun buildMonthlySavedDescription(
    promotionType: PromotionType,
    monthlySavedAmount: Int,
): String {
    return if (monthlySavedAmount > 0) {
        "매월 ${formatWon(monthlySavedAmount)} 절감할 수 있어요."
    } else {
        buildFallbackSummaryDescription(promotionType)
    }
}

private fun buildDetailPriceContentType(
    promotionType: PromotionType,
    priceSummary: PromotionPriceSummaryUiModel,
): PromotionDetailPriceContentType {
    return if (promotionType == PromotionType.CardBenefit) {
        PromotionDetailPriceContentType.SummaryHighlight
    } else if (priceSummary.hasPriceComparison) {
        PromotionDetailPriceContentType.PriceComparison
    } else {
        PromotionDetailPriceContentType.None
    }
}

private fun buildFallbackSummaryDescription(
    promotionType: PromotionType,
): String {
    return when (promotionType) {
        PromotionType.CardBenefit -> "카드 발급 및 실적 조건을 확인한 뒤 혜택을 적용할 수 있어요."
        PromotionType.Bundle -> "여러 서비스를 함께 이용할 때 받을 수 있는 번들 혜택입니다."
        PromotionType.Promo -> "구독 서비스에서 제공하는 프로모션 혜택입니다."
    }
}

private fun buildValidityPeriodLabel(
    startsAt: String,
    endsAt: String,
): String? {
    val startLabel = startsAt.toLocalDateOrNull()?.format(validityDateFormatter)
    val endLabel = endsAt.toLocalDateOrNull()?.format(validityDateFormatter)

    return when {
        startLabel != null && endLabel != null -> "$startLabel ~ $endLabel"
        endLabel != null -> "~ $endLabel"
        else -> null
    }
}

internal fun formatWon(amount: Int): String {
    return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원"
}

private fun String.toLocalDateTimeOrNull(): LocalDateTime? {
    return runCatching { LocalDateTime.parse(this) }.getOrNull()
}

private fun String.toLocalDateOrNull(): LocalDate? {
    return runCatching { LocalDateTime.parse(this).toLocalDate() }
        .recoverCatching { LocalDate.parse(this) }
        .getOrNull()
}

private val validityDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
private val recommendationCategoryOrder = listOf("OTT", "MUSIC", "AI")
