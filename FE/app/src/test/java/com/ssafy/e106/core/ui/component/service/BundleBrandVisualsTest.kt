package com.ssafy.e106.core.ui.component.service

import org.junit.Assert.assertEquals
import org.junit.Test

class BundleBrandVisualsTest {

    @Test
    fun `shows two logos without overflow badge for two-service bundle`() {
        val layout = resolveBundleBrandClusterLayout(
            services = sampleServices(2),
            maxVisibleLogos = 3,
            showOverflowCount = true,
        )

        assertEquals(2, layout.visibleServices.size)
        assertEquals(0, layout.hiddenCount)
    }

    @Test
    fun `shows two logos and overflow badge for three-service bundle`() {
        val layout = resolveBundleBrandClusterLayout(
            services = sampleServices(3),
            maxVisibleLogos = 3,
            showOverflowCount = true,
        )

        assertEquals(2, layout.visibleServices.size)
        assertEquals(1, layout.hiddenCount)
    }

    @Test
    fun `keeps original three-logo behavior when overflow badge is disabled`() {
        val layout = resolveBundleBrandClusterLayout(
            services = sampleServices(4),
            maxVisibleLogos = 3,
            showOverflowCount = false,
        )

        assertEquals(3, layout.visibleServices.size)
        assertEquals(0, layout.hiddenCount)
    }

    private fun sampleServices(count: Int): List<BundleBrandVisualItem> {
        return (1..count).map { index ->
            BundleBrandVisualItem(
                name = "Service $index",
                code = "SERVICE_$index",
            )
        }
    }
}
