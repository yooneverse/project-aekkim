package com.ssafy.e106.data.repository

import com.ssafy.e106.core.network.currentBaseUrl

internal fun resolveMediaUrl(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (value.startsWith("http://") || value.startsWith("https://")) {
        return value
    }

    val baseUrl = currentBaseUrl().trim().let { base ->
        if (base.endsWith("/")) base.dropLast(1) else base
    }

    return if (value.startsWith("/")) {
        "$baseUrl$value"
    } else {
        "$baseUrl/$value"
    }
}
