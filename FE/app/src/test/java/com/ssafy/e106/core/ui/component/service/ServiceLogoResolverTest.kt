package com.ssafy.e106.core.ui.component.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServiceLogoResolverTest {

    @Test
    fun `keeps explicit absolute logo url for unknown service`() {
        val resolved = resolveServiceLogoUrl(
            serviceName = "Unknown Service",
            logoUrl = "https://cdn.example.com/unknown.png",
            baseUrl = "http://10.0.2.2:8080/",
        )

        assertEquals("https://cdn.example.com/unknown.png", resolved)
    }

    @Test
    fun `maps known music service name to bundled asset path`() {
        val resolved = resolveServiceLogoUrl(
            serviceName = "Spotify",
            logoUrl = null,
            baseUrl = "http://10.0.2.2:8080/",
        )

        assertEquals("file:///android_asset/services/SPOTIFY.png", resolved)
    }

    @Test
    fun `maps known service code to bundled asset path`() {
        val resolved = resolveServiceLogoUrl(
            serviceName = "",
            serviceCode = "YOUTUBE_MUSIC",
            logoUrl = null,
            baseUrl = "http://10.0.2.2:8080/",
        )

        assertEquals("file:///android_asset/services/YOUTUBE_MUSIC.png", resolved)
    }

    @Test
    fun `prefers bundled asset when backend service asset url is provided`() {
        val resolved = resolveServiceLogoUrl(
            serviceName = "Claude",
            logoUrl = "http://10.0.2.2:8080/assets/services/CLAUDE.png",
            baseUrl = "http://10.0.2.2:8080/",
        )

        assertEquals("file:///android_asset/services/CLAUDE.png", resolved)
    }

    @Test
    fun `returns null for unknown service without logo url`() {
        val resolved = resolveServiceLogoUrl(
            serviceName = "Unknown Service",
            logoUrl = null,
            baseUrl = "http://10.0.2.2:8080/",
        )

        assertNull(resolved)
    }
}
