package com.ssafy.e106.core.ui.component.service

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

@Composable
fun ServiceBrandIcon(
    serviceName: String,
    logoUrl: String?,
    serviceCode: String? = null,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    imagePadding: Dp = 8.dp,
    contentScale: ContentScale = ContentScale.Fit,
    fallbackLabel: String? = null,
    fallbackTextStyle: TextStyle = MaterialTheme.typography.titleLarge,
    fallbackTextColor: Color = MaterialTheme.colorScheme.onSurface,
    fallbackFontWeight: FontWeight = FontWeight.Bold,
) {
    val resolvedFallbackLabel = fallbackLabel
        ?.trim()
        ?.takeIf { label -> label.isNotEmpty() }
        ?: serviceName.trim().firstOrNull()?.toString()?.uppercase()
        ?: "?"
    val resolvedLogoUrl = resolveServiceLogoUrl(
        serviceName = serviceName,
        logoUrl = logoUrl,
        serviceCode = serviceCode,
    )

    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        border = border,
    ) {
        if (resolvedLogoUrl == null) {
            BrandFallbackText(
                label = resolvedFallbackLabel,
                textStyle = fallbackTextStyle,
                textColor = fallbackTextColor,
                fontWeight = fallbackFontWeight,
            )
            return@Surface
        }

        SubcomposeAsyncImage(
            model = resolvedLogoUrl,
            contentDescription = serviceName.takeIf { it.isNotBlank() }?.let { "$it 로고" },
            modifier = Modifier
                .fillMaxSize()
                .padding(imagePadding),
            contentScale = contentScale,
        ) {
            when (painter.state) {
                is AsyncImagePainter.State.Error -> BrandFallbackText(
                    label = resolvedFallbackLabel,
                    textStyle = fallbackTextStyle,
                    textColor = fallbackTextColor,
                    fontWeight = fallbackFontWeight,
                )

                else -> SubcomposeAsyncImageContent()
            }
        }
    }
}

@Composable
private fun BrandFallbackText(
    label: String,
    textStyle: TextStyle,
    textColor: Color,
    fontWeight: FontWeight,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = textStyle,
            color = textColor,
            fontWeight = fontWeight,
        )
    }
}
