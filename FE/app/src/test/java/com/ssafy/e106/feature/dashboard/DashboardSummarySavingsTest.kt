package com.ssafy.e106.feature.dashboard

import com.ssafy.e106.data.repository.PromotionFeedData
import com.ssafy.e106.data.repository.PromotionPriceData
import com.ssafy.e106.data.repository.PromotionRecommendationCategoryData
import com.ssafy.e106.data.repository.PromotionRecommendationData
import com.ssafy.e106.data.repository.PromotionRecommendationGroupType
import com.ssafy.e106.data.repository.PromotionServiceData
import com.ssafy.e106.data.repository.PromotionType
import com.ssafy.e106.data.repository.SubscriptionOverview
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardSummarySavingsTest {

    @Test
    fun toSubscriptionCardItem_maps_expected_saving_amount() {
        val overview = subscriptionOverview(expectedSavingAmount = 4_500)

        val item = overview.toSubscriptionCardItem()

        assertEquals(4_500, item.expectedSavingAmount)
    }

    @Test
    fun totalExpectedSavingAmount_sums_only_non_negative_values() {
        val items = listOf(
            subscriptionOverview(expectedSavingAmount = 3_000).toSubscriptionCardItem(),
            subscriptionOverview(subscriptionId = 2L, expectedSavingAmount = null).toSubscriptionCardItem(),
            subscriptionOverview(subscriptionId = 3L, expectedSavingAmount = -700).toSubscriptionCardItem(),
            subscriptionOverview(subscriptionId = 4L, expectedSavingAmount = 2_400).toSubscriptionCardItem(),
        )

        assertEquals(5_400, items.totalExpectedSavingAmount())
    }

    @Test
    fun calculatePromotionFeedExpectedSavingAmount_picks_best_non_overlapping_combo() {
        val items = listOf(
            subscriptionOverview(
                subscriptionId = 1L,
                serviceId = 101L,
                serviceName = "Netflix",
                expectedSavingAmount = null,
            ).toSubscriptionCardItem(),
            subscriptionOverview(
                subscriptionId = 2L,
                serviceId = 102L,
                serviceName = "Tving",
                expectedSavingAmount = null,
            ).toSubscriptionCardItem(),
            subscriptionOverview(
                subscriptionId = 3L,
                serviceId = 103L,
                serviceName = "Spotify",
                expectedSavingAmount = null,
            ).toSubscriptionCardItem(),
        )
        val promotionFeed = PromotionFeedData(
            nickname = "tester",
            categories = listOf(
                PromotionRecommendationCategoryData(
                    category = "OTT",
                    bundles = listOf(
                        recommendation(
                            promotionId = 1001L,
                            monthlySavedAmount = 10_000,
                            serviceIds = listOf(101L, 102L),
                        ),
                    ),
                    promotions = listOf(
                        recommendation(
                            promotionId = 1002L,
                            monthlySavedAmount = 5_000,
                            serviceIds = listOf(101L),
                        ),
                        recommendation(
                            promotionId = 1003L,
                            monthlySavedAmount = 4_000,
                            serviceIds = listOf(102L),
                        ),
                    ),
                    cardBenefits = listOf(
                        recommendation(
                            promotionId = 1004L,
                            monthlySavedAmount = 3_000,
                            serviceIds = listOf(103L),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(13_000, items.calculatePromotionFeedExpectedSavingAmount(promotionFeed))
    }

    private fun subscriptionOverview(
        subscriptionId: Long = 1L,
        serviceId: Long = 101L,
        serviceName: String = "Netflix",
        expectedSavingAmount: Int?,
    ): SubscriptionOverview {
        return SubscriptionOverview(
            subscriptionId = subscriptionId,
            serviceId = serviceId,
            servicePlanId = 201L,
            serviceName = serviceName,
            planName = "Basic",
            billingCycle = "MONTHLY",
            monthlyPrice = 13_500,
            expectedSavingAmount = expectedSavingAmount,
            savingLabel = expectedSavingAmount
                ?.takeIf { amount -> amount > 0 }
                ?.let { amount -> "save-$amount" },
        )
    }

    private fun recommendation(
        promotionId: Long,
        monthlySavedAmount: Int,
        serviceIds: List<Long>,
    ): PromotionRecommendationData {
        return PromotionRecommendationData(
            promotionId = promotionId,
            groupType = PromotionRecommendationGroupType.SubscriptionList,
            promotionType = PromotionType.Bundle,
            headline = "headline",
            summary = "summary",
            price = PromotionPriceData(
                originalPrice = monthlySavedAmount + 1_000,
                discountPrice = 1_000,
                monthlySavedAmount = monthlySavedAmount,
            ),
            services = serviceIds.map { serviceId ->
                PromotionServiceData(
                    serviceId = serviceId,
                    name = "service-$serviceId",
                )
            },
            startsAt = "2026-03-01T00:00:00",
            endsAt = "2099-12-31T23:59:59",
        )
    }
}
