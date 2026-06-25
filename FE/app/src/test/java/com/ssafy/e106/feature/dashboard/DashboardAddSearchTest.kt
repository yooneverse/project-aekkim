package com.ssafy.e106.feature.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardAddSearchTest {

    @Test
    fun filterDashboardAddTargets_matches_service_and_bundles_by_included_service_name() {
        val netflix = service(
            serviceId = 1L,
            code = "NETFLIX",
            name = "넷플릭스",
            aliases = setOf("Netflix"),
        )
        val disney = service(
            serviceId = 2L,
            code = "DISNEY_PLUS",
            name = "디즈니플러스",
            aliases = setOf("디즈니+"),
        )
        val tving = service(
            serviceId = 3L,
            code = "TVING",
            name = "티빙",
        )

        val results = filterDashboardAddTargets(
            query = "넷플",
            services = listOf(netflix, disney, tving),
            bundles = listOf(
                bundle(
                    code = "DISNEY_PLUS_NETFLIX",
                    name = "유독 더블 스트리밍 할인",
                    includedServices = listOf(netflix, disney),
                ),
                bundle(
                    code = "TVING_WAVVE_STANDARD",
                    name = "더블 스탠다드",
                    includedServices = listOf(tving),
                ),
            ),
        )

        assertEquals(listOf("넷플릭스"), results.services.map { service -> service.name })
        assertEquals(
            listOf("DISNEY_PLUS_NETFLIX"),
            results.bundles.map { bundle -> bundle.code },
        )
    }

    @Test
    fun filterDashboardAddTargets_normalizes_whitespace_and_case() {
        val chatGpt = service(
            serviceId = 11L,
            code = "CHATGPT",
            name = "ChatGPT",
            aliases = setOf("챗지피티"),
        )

        val results = filterDashboardAddTargets(
            query = " chat gpt ",
            services = listOf(chatGpt),
            bundles = emptyList(),
        )

        assertTrue(results.services.any { service -> service.code == "CHATGPT" })
    }

    private fun service(
        serviceId: Long,
        code: String,
        name: String,
        aliases: Set<String> = emptySet(),
    ): DashboardServiceItem {
        return DashboardServiceItem(
            serviceId = serviceId,
            code = code,
            name = name,
            aliases = aliases,
        )
    }

    private fun bundle(
        code: String,
        name: String,
        includedServices: List<DashboardServiceItem>,
    ): DashboardBundleItem {
        return DashboardBundleItem(
            code = code,
            name = name,
            planName = name,
            billingCycle = "MONTHLY",
            monthlyPrice = 9_900,
            includedServices = includedServices,
        )
    }
}
