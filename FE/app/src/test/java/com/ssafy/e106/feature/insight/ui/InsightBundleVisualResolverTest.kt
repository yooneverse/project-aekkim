package com.ssafy.e106.feature.insight.ui

import com.ssafy.e106.feature.insight.InsightSubscriptionItemUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightBundleVisualResolverTest {

    @Test
    fun resolveBundleBrandVisuals_matches_bundle_alias_when_bundle_code_is_missing() {
        val visuals = bundleItem(
            serviceName = "더블 광고형 스탠다드",
            planName = "더블 광고형 스탠다드",
            bundleCode = null,
        ).resolveBundleBrandVisuals()

        assertEquals(listOf("TVING", "WAVVE"), visuals.mapNotNull { it.code })
    }

    @Test
    fun resolveBundleBrandVisuals_matches_bundle_code_variants_from_backend() {
        val visuals = bundleItem(
            serviceName = "더블 광고형 스탠다드",
            planName = "더블 광고형 스탠다드",
            bundleCode = "TVING_WAVVE_STANDARD",
        ).resolveBundleBrandVisuals()

        assertEquals(listOf("TVING", "WAVVE"), visuals.mapNotNull { it.code })
    }

    @Test
    fun resolveBundleBrandVisuals_matches_three_service_bundle_alias() {
        val visuals = bundleItem(
            serviceName = "3 PACK",
            planName = "3 PACK",
            bundleCode = null,
        ).resolveBundleBrandVisuals()

        assertEquals(
            listOf("TVING", "DISNEY_PLUS", "WAVVE"),
            visuals.mapNotNull { it.code },
        )
    }

    private fun bundleItem(
        serviceName: String,
        planName: String,
        bundleCode: String?,
    ): InsightSubscriptionItemUiModel {
        return InsightSubscriptionItemUiModel(
            subscriptionId = 1L,
            serviceName = serviceName,
            planName = planName,
            subscriptionType = "BUNDLE",
            bundleCode = bundleCode,
            category = "OTT",
            logoUrl = null,
            monthlyPrice = 7000,
            totalUsedMinutes = 120,
            usedDays = 3,
            lastUsedDateLabel = null,
            hourlyCost = null,
            nudgeMessage = null,
        )
    }
}
