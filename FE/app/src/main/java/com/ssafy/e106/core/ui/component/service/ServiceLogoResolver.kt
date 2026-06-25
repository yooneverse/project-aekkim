package com.ssafy.e106.core.ui.component.service

import com.ssafy.e106.core.network.currentBaseUrl
import com.ssafy.e106.core.network.normalizeBaseUrl
import java.util.Locale

private const val bundledServiceAssetPrefix = "file:///android_asset/services/"

private data class ServiceLogoAsset(
    val code: String,
    val aliases: Set<String>,
)

private val knownServiceLogoAssets = listOf(
    ServiceLogoAsset("NETFLIX", setOf("넷플릭스", "netflix")),
    ServiceLogoAsset("TVING", setOf("티빙", "tving")),
    ServiceLogoAsset("WAVVE", setOf("웨이브", "wavve")),
    ServiceLogoAsset("WATCHA", setOf("왓챠", "watcha")),
    ServiceLogoAsset("COUPANG_PLAY", setOf("쿠팡플레이", "coupang play", "coupangplay")),
    ServiceLogoAsset("DISNEY_PLUS", setOf("디즈니플러스", "디즈니+", "disney plus", "disney+")),
    ServiceLogoAsset("MELON", setOf("멜론", "melon")),
    ServiceLogoAsset("BUGS", setOf("벅스", "bugs")),
    ServiceLogoAsset("YOUTUBE_MUSIC", setOf("유튜브뮤직", "youtube music", "youtubemusic")),
    ServiceLogoAsset("SPOTIFY", setOf("스포티파이", "spotify")),
    ServiceLogoAsset("CHATGPT", setOf("chatgpt", "챗지피티", "챗gpt")),
    ServiceLogoAsset("GEMINI", setOf("gemini", "제미나이")),
    ServiceLogoAsset("CLAUDE", setOf("claude", "클로드")),
)

private val assetCodeByNormalizedKey: Map<String, String> = buildMap {
    knownServiceLogoAssets.forEach { asset ->
        put(normalizeServiceLogoKey(asset.code), asset.code)
        asset.aliases.forEach { alias ->
            put(normalizeServiceLogoKey(alias), asset.code)
        }
    }
}

internal fun resolveServiceLogoUrl(
    serviceName: String,
    logoUrl: String?,
    serviceCode: String? = null,
    baseUrl: String = currentBaseUrl(),
): String? {
    val explicitLogoUrl = logoUrl?.trim().orEmpty()
    val bundledAssetCode = resolveBundledServiceAssetCode(
        serviceName = serviceName,
        serviceCode = serviceCode,
        logoUrl = explicitLogoUrl,
    )
    if (bundledAssetCode != null) {
        return toBundledServiceAssetUrl(bundledAssetCode)
    }
    if (explicitLogoUrl.isBlank()) {
        return null
    }

    return when {
        explicitLogoUrl.startsWith("http://") || explicitLogoUrl.startsWith("https://") -> explicitLogoUrl
        explicitLogoUrl.startsWith("file:///") ||
            explicitLogoUrl.startsWith("content://") ||
            explicitLogoUrl.startsWith("android.resource://") -> explicitLogoUrl
        explicitLogoUrl.startsWith("/") -> normalizeResolvedBaseUrl(baseUrl) + explicitLogoUrl.removePrefix("/")
        else -> normalizeResolvedBaseUrl(baseUrl) + explicitLogoUrl
    }
}

private fun resolveBundledServiceAssetCode(
    serviceName: String,
    serviceCode: String?,
    logoUrl: String,
): String? {
    return sequenceOf(
        serviceCode,
        extractLogoFileStem(logoUrl),
        serviceName,
    )
        .map(::normalizeServiceLogoKey)
        .mapNotNull(assetCodeByNormalizedKey::get)
        .firstOrNull()
}

private fun extractLogoFileStem(logoUrl: String): String? {
    if (logoUrl.isBlank()) {
        return null
    }

    val sanitizedPath = logoUrl
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('/')

    if (sanitizedPath.isBlank()) {
        return null
    }

    return sanitizedPath.substringBeforeLast('.', sanitizedPath)
}

private fun toBundledServiceAssetUrl(assetCode: String): String {
    return "$bundledServiceAssetPrefix$assetCode.png"
}

private fun normalizeResolvedBaseUrl(baseUrl: String): String {
    return normalizeBaseUrl(raw = baseUrl)
}

private fun normalizeServiceLogoKey(value: String?): String {
    return value
        .orEmpty()
        .trim()
        .uppercase(Locale.ROOT)
        .filter { char -> char.isLetterOrDigit() }
}
