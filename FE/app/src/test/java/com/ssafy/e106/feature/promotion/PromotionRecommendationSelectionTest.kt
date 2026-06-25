package com.ssafy.e106.feature.promotion

import com.ssafy.e106.data.repository.PromotionFeedData
import com.ssafy.e106.data.repository.PromotionPriceData
import com.ssafy.e106.data.repository.PromotionRecommendationCategoryData
import com.ssafy.e106.data.repository.PromotionRecommendationData
import com.ssafy.e106.data.repository.PromotionRecommendationGroupType
import com.ssafy.e106.data.repository.PromotionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromotionRecommendationSelectionTest {

    @Test
    fun availableTabs_returns_only_tabs_with_items() {
        val category = PromotionRecommendationCategoryUiModel(
            categoryKey = "OTT",
            categoryLabel = "OTT",
            bundles = emptyList(),
            promotions = listOf(promotionCard(id = 1L, type = PromotionType.Promo)),
            cardBenefits = listOf(promotionCard(id = 2L, type = PromotionType.CardBenefit)),
        )

        assertEquals(
            listOf(
                PromotionRecommendationTabUiType.Promo,
                PromotionRecommendationTabUiType.CardBenefit,
            ),
            category.availableTabs,
        )
    }

    @Test
    fun toListScreenState_returns_empty_when_every_category_is_empty() {
        val feed = PromotionFeedData(
            nickname = "사용자",
            categories = listOf(
                PromotionRecommendationCategoryData(
                    category = "OTT",
                    bundles = emptyList(),
                    promotions = emptyList(),
                    cardBenefits = emptyList(),
                ),
            ),
        )

        assertTrue(feed.toListScreenState() is PromotionListScreenState.Empty)
    }

    @Test
    fun firstSelection_prefers_first_category_and_tab_with_items() {
        val screenState = PromotionFeedData(
            nickname = "사용자",
            categories = listOf(
                PromotionRecommendationCategoryData(
                    category = "MUSIC",
                    bundles = emptyList(),
                    promotions = listOf(recommendation(id = 10L, type = PromotionType.Promo)),
                    cardBenefits = emptyList(),
                ),
            ),
        ).toListScreenState()

        assertEquals("MUSIC", screenState.firstCategoryKeyOrNull())
        assertEquals(
            PromotionRecommendationTabUiType.Promo,
            screenState.firstAvailableTabOrDefault("OTT"),
        )
    }

    private fun promotionCard(
        id: Long,
        type: PromotionType,
    ): PromotionCardUiModel {
        return PromotionCardUiModel(
            promotionId = id,
            promotionType = type,
            headline = "추천",
            summary = "요약",
            priceSummary = PromotionPriceSummaryUiModel(
                originalPriceLabel = "10,000원",
                discountPriceLabel = "7,000원",
                monthlySavedLabel = "3,000원",
            ),
            savingsBadgeText = "월 3,000원 절감",
        )
    }

    private fun recommendation(
        id: Long,
        type: PromotionType,
    ): PromotionRecommendationData {
        return PromotionRecommendationData(
            promotionId = id,
            groupType = PromotionRecommendationGroupType.Conditional,
            promotionType = type,
            headline = "추천",
            summary = "요약",
            price = PromotionPriceData(
                originalPrice = 10_000,
                discountPrice = 7_000,
                monthlySavedAmount = 3_000,
            ),
            startsAt = "2026-03-01T00:00:00",
            endsAt = "2099-12-31T23:59:59",
        )
    }
}
