package com.ssafy.e106.core.ui.component.image

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
import com.ssafy.e106.core.ui.component.service.resolveServiceLogoUrl

@Composable
fun ServiceLogoBadge(
    serviceName: String,
    logoUrl: String?,
    serviceCode: String? = null,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    fallbackTextStyle: TextStyle = MaterialTheme.typography.titleLarge,
    fallbackTextColor: Color = MaterialTheme.colorScheme.onSurface,
    fallbackFontWeight: FontWeight = FontWeight.Bold,
    contentPadding: Dp = 8.dp,
) {
    val resolvedLogoUrl = resolveServiceLogoUrl(
        serviceName = serviceName,
        logoUrl = logoUrl,
        serviceCode = serviceCode,
    )

    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        border = border,
    ) {
        if (resolvedLogoUrl == null) {
            ServiceInitialText(
                serviceName = serviceName,
                style = fallbackTextStyle,
                color = fallbackTextColor,
                fontWeight = fallbackFontWeight,
            )
            return@Surface
        }

        SubcomposeAsyncImage(
            model = resolvedLogoUrl,
            contentDescription = "$serviceName 로고",
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentScale = ContentScale.Fit,
        ) {
            when (painter.state) {
                is AsyncImagePainter.State.Error -> ServiceInitialText(
                    serviceName = serviceName,
                    style = fallbackTextStyle,
                    color = fallbackTextColor,
                    fontWeight = fallbackFontWeight,
                )

                else -> SubcomposeAsyncImageContent()
            }
        }
    }
}

@Composable
private fun ServiceInitialText(
    serviceName: String,
    style: TextStyle,
    color: Color,
    fontWeight: FontWeight,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = serviceName.firstOrNull()?.toString() ?: "?",
            style = style,
            color = color,
            fontWeight = fontWeight,
        )
    }
}
