package com.ssafy.e106.feature.promotion

import com.ssafy.e106.data.repository.PromotionType

data class PromotionPriceSummaryUiModel(
    val originalPriceLabel: String?,
    val discountPriceLabel: String?,
    val monthlySavedLabel: String,
) {
    val hasPriceComparison: Boolean
        get() = !originalPriceLabel.isNullOrBlank() && !discountPriceLabel.isNullOrBlank()
}

data class PromotionListUiState(
    val nickname: String = "사용자",
    val selectedCategoryKey: String? = null,
    val selectedTab: PromotionRecommendationTabUiType = PromotionRecommendationTabUiType.Bundle,
    val isExpanded: Boolean = false,
    val screenState: PromotionListScreenState = PromotionListScreenState.Loading,
)

sealed interface PromotionListScreenState {
    data object Loading : PromotionListScreenState
    data class Success(val categories: List<PromotionRecommendationCategoryUiModel>) : PromotionListScreenState
    data object Empty : PromotionListScreenState
    data class Error(val message: String) : PromotionListScreenState
}

data class PromotionRecommendationCategoryUiModel(
    val categoryKey: String,
    val categoryLabel: String,
    val bundles: List<PromotionCardUiModel>,
    val promotions: List<PromotionCardUiModel>,
    val cardBenefits: List<PromotionCardUiModel>,
) {
    val hasAnyItems: Boolean
        get() = bundles.isNotEmpty() || promotions.isNotEmpty() || cardBenefits.isNotEmpty()

    val availableTabs: List<PromotionRecommendationTabUiType>
        get() = buildList {
            if (bundles.isNotEmpty()) add(PromotionRecommendationTabUiType.Bundle)
            if (promotions.isNotEmpty()) add(PromotionRecommendationTabUiType.Promo)
            if (cardBenefits.isNotEmpty()) add(PromotionRecommendationTabUiType.CardBenefit)
        }

    fun itemsFor(tab: PromotionRecommendationTabUiType): List<PromotionCardUiModel> {
        return when (tab) {
            PromotionRecommendationTabUiType.Bundle -> bundles
            PromotionRecommendationTabUiType.Promo -> promotions
            PromotionRecommendationTabUiType.CardBenefit -> cardBenefits
        }
    }
}

enum class PromotionRecommendationTabUiType(
    val label: String,
) {
    Bundle("번들"),
    Promo("프로모션"),
    CardBenefit("카드 혜택"),
}

data class PromotionCardUiModel(
    val promotionId: Long,
    val promotionType: PromotionType,
    val headline: String,
    val summary: String?,
    val priceSummary: PromotionPriceSummaryUiModel,
    val savingsBadgeText: String,
    val recommendationReasons: List<String> = emptyList(),
    val conditionDescription: String? = null,
    val sourceUrl: String? = null,
    val imageUrl: String? = null,
    val services: List<PromotionServiceLogoUiModel> = emptyList(),
)

data class PromotionServiceLogoUiModel(
    val serviceId: Long,
    val label: String,
    val code: String? = null,
    val logoUrl: String? = null,
)

sealed interface PromotionListUiEffect {
    data class NavigateToPromotionDetail(val promotionId: Long) : PromotionListUiEffect
}

sealed interface PromotionListIntent {
    data object LoadPromotions : PromotionListIntent
    data object RetryLoad : PromotionListIntent
    data class SelectCategory(val categoryKey: String) : PromotionListIntent
    data class SelectTab(val tab: PromotionRecommendationTabUiType) : PromotionListIntent
    data object ToggleExpanded : PromotionListIntent
    data class OpenPromotionDetail(val promotionId: Long) : PromotionListIntent
}

data class PromotionDetailUiState(
    val promotionId: Long? = null,
    val screenState: PromotionDetailScreenState = PromotionDetailScreenState.Loading,
)

sealed interface PromotionDetailScreenState {
    data object Loading : PromotionDetailScreenState
    data class Success(val promotion: PromotionDetailUiModel) : PromotionDetailScreenState
    data class Expired(val promotion: PromotionDetailUiModel) : PromotionDetailScreenState
    data class NoLink(val promotion: PromotionDetailUiModel) : PromotionDetailScreenState
    data class Error(val message: String) : PromotionDetailScreenState
}

enum class PromotionDetailPriceContentType {
    PriceComparison,
    SummaryHighlight,
    None,
}

data class PromotionDetailUiModel(
    val promotionId: Long,
    val promotionType: PromotionType,
    val headline: String,
    val summary: String?,
    val priceSummary: PromotionPriceSummaryUiModel,
    val savingsBadgeText: String,
    val priceContentType: PromotionDetailPriceContentType,
    val priceSummaryDescription: String,
    val monthlySavedDescription: String,
    val yearlySavedTitle: String,
    val yearlySavedLabel: String,
    val yearlyBenefitDescription: String,
    val conditionDescription: String? = null,
    val detailDescription: String,
    val applySteps: List<String>,
    val actionButtonText: String,
    val sourceUrl: String? = null,
    val imageUrl: String? = null,
    val services: List<PromotionServiceLogoUiModel> = emptyList(),
    val validityPeriodLabel: String? = null,
)

sealed interface PromotionDetailUiEffect {
    data class OpenExternalLink(val url: String) : PromotionDetailUiEffect
    data class ShowToast(val message: String) : PromotionDetailUiEffect
}

sealed interface PromotionDetailIntent {
    data class LoadPromotionDetail(val promotionId: Long) : PromotionDetailIntent
    data object RetryLoad : PromotionDetailIntent
    data object OpenSignupLink : PromotionDetailIntent
}
