package com.ssafy.e106.core.network

import android.os.Build
import com.ssafy.e106.BuildConfig

private const val FALLBACK_EMULATOR_BASE_URL = "http://10.0.2.2:8080/"

fun currentBaseUrl(): String {
    val runningOnEmulator = isProbablyEmulator()
    val preferredBaseUrl = when {
        !BuildConfig.DEBUG -> BuildConfig.BASE_URL
        runningOnEmulator -> BuildConfig.BASE_URL_EMULATOR
        else -> BuildConfig.BASE_URL_DEVICE
    }
    val fallbackBaseUrl = when {
        !BuildConfig.DEBUG -> BuildConfig.BASE_URL
        runningOnEmulator -> BuildConfig.BASE_URL_EMULATOR
        else -> BuildConfig.BASE_URL_DEVICE.ifBlank { BuildConfig.BASE_URL_EMULATOR }
    }

    return normalizeBaseUrl(preferredBaseUrl, fallbackBaseUrl)
}

fun normalizeBaseUrl(raw: String, fallback: String = FALLBACK_EMULATOR_BASE_URL): String {
    val trimmed = raw.trim()
    val candidate = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        fallback.trim()
    }
    val normalized = if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
        candidate
    } else {
        FALLBACK_EMULATOR_BASE_URL
    }
    return if (normalized.endsWith("/")) normalized else "$normalized/"
}

private fun isProbablyEmulator(): Boolean {
    return Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.MODEL.contains("google_sdk") ||
        Build.MODEL.contains("Emulator") ||
        Build.MODEL.contains("Android SDK built for x86") ||
        Build.MANUFACTURER.contains("Genymotion") ||
        Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
        Build.PRODUCT == "google_sdk"
}
