package com.ssafy.e106.core.ui.component.service

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.e106.core.ui.component.image.ServiceLogoBadge

data class BundleBrandVisualItem(
    val name: String,
    val code: String? = null,
    val logoUrl: String? = null,
)

fun List<BundleBrandVisualItem>.resolveBrandGradientColors(
    defaultStart: Color,
    defaultEnd: Color,
): List<Color> {
    val paletteByService = map { service -> service.resolveBrandPalette() }
        .filter { palette -> palette.isNotEmpty() }
        .take(2)

    val brandColors = paletteByService
        .flatMap { palette -> palette }
        .distinct()

    return when {
        brandColors.size >= 2 -> brandColors.take(4)
        brandColors.size == 1 -> listOf(brandColors.first(), defaultEnd)
        else -> listOf(defaultStart, defaultEnd)
    }
}

@Composable
fun BundleBrandLogoCluster(
    services: List<BundleBrandVisualItem>,
    modifier: Modifier = Modifier,
    logoSize: Dp = 38.dp,
    overlap: Dp = 8.dp,
    maxVisibleLogos: Int = 3,
    showOverflowCount: Boolean = false,
    logoContainerColor: Color = Color.White.copy(alpha = 0.94f),
    logoBorder: BorderStroke? = BorderStroke(1.dp, Color.White.copy(alpha = 0.90f)),
) {
    val layout = resolveBundleBrandClusterLayout(
        services = services,
        maxVisibleLogos = maxVisibleLogos,
        showOverflowCount = showOverflowCount,
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-overlap)),
    ) {
        layout.visibleServices.forEach { service ->
            ServiceLogoBadge(
                serviceName = service.name,
                logoUrl = service.logoUrl,
                serviceCode = service.code,
                modifier = Modifier.size(logoSize),
                shape = MaterialTheme.shapes.extraLarge,
                containerColor = logoContainerColor,
                border = logoBorder,
                contentPadding = 7.dp,
            )
        }

        if (layout.hiddenCount > 0) {
            Surface(
                modifier = Modifier.size(logoSize),
                shape = MaterialTheme.shapes.extraLarge,
                color = logoContainerColor,
                border = logoBorder,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+${layout.hiddenCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

internal data class BundleBrandClusterLayout(
    val visibleServices: List<BundleBrandVisualItem>,
    val hiddenCount: Int,
)

internal fun resolveBundleBrandClusterLayout(
    services: List<BundleBrandVisualItem>,
    maxVisibleLogos: Int,
    showOverflowCount: Boolean,
): BundleBrandClusterLayout {
    val safeMaxVisibleLogos = maxVisibleLogos.coerceAtLeast(1)
    val reservesOverflowSlot = showOverflowCount && safeMaxVisibleLogos > 1
    val visibleLogoSlots = if (reservesOverflowSlot) {
        safeMaxVisibleLogos - 1
    } else {
        safeMaxVisibleLogos
    }
    val visibleServices = services.take(visibleLogoSlots)
    val hiddenCount = if (reservesOverflowSlot) {
        (services.size - visibleServices.size).coerceAtLeast(0)
    } else {
        0
    }

    return BundleBrandClusterLayout(
        visibleServices = visibleServices,
        hiddenCount = hiddenCount,
    )
}

private fun BundleBrandVisualItem.resolveBrandPalette(): List<Color> {
    val normalizedKey = listOfNotNull(
        code,
        name,
        logoUrl.extractAssetStem(),
    ).joinToString(separator = " ").lowercase()

    return when {
        "netflix" in normalizedKey -> listOf(
            Color(0xFF0F0F0F),
            Color(0xFFE50914),
        )

        "tving" in normalizedKey -> listOf(
            Color(0xFFFFFFFF),
            Color(0xFFFF2E2E),
        )

        "wavve" in normalizedKey -> listOf(
            Color(0xFF0A163D),
            Color(0xFF3D7CFF),
        )

        "disney" in normalizedKey -> listOf(
            Color(0xFF0B1D3A),
            Color(0xFF00D1FF),
        )

        "watcha" in normalizedKey -> listOf(
            Color(0xFF111111),
            Color(0xFFFF2F6E),
        )

        "coupang" in normalizedKey -> listOf(
            Color(0xFF2A58FF),
            Color(0xFFFF5555),
        )

        "youtube" in normalizedKey -> listOf(
            Color(0xFFFFFFFF),
            Color(0xFFFF0000),
        )

        "spotify" in normalizedKey -> listOf(
            Color(0xFF121212),
            Color(0xFF1ED760),
        )

        "melon" in normalizedKey -> listOf(
            Color(0xFF00D344),
            Color(0xFF005B2A),
        )

        "bugs" in normalizedKey -> listOf(
            Color(0xFF1B2CFF),
            Color(0xFF7A8BFF),
        )

        "chatgpt" in normalizedKey -> listOf(
            Color(0xFF0F1714),
            Color(0xFF10A37F),
        )

        "claude" in normalizedKey -> listOf(
            Color(0xFF241A14),
            Color(0xFFD97757),
        )

        "gemini" in normalizedKey -> listOf(
            Color(0xFF1A1C3A),
            Color(0xFF5AA7FF),
        )

        else -> emptyList()
    }
}

private fun String?.extractAssetStem(): String? {
    if (this.isNullOrBlank()) return null

    val normalizedPath = substringBefore('?')
        .substringAfterLast('/')
        .substringBeforeLast('.')
        .trim()

    return normalizedPath.takeIf { it.isNotEmpty() }
}
