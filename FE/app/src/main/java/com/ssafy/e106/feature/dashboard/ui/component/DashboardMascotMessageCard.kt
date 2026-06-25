package com.ssafy.e106.feature.dashboard.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ssafy.e106.core.ui.theme.AekkimThemeTokens

@Composable
fun DashboardMascotMessageCard(
    message: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    onClick: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    trailingContentDescription: String? = null,
    onTrailingIconClick: (() -> Unit)? = null,
) {
    val containerColor = lerp(
        start = MaterialTheme.colorScheme.surface,
        stop = MaterialTheme.colorScheme.primary,
        fraction = 0.12f,
    )
    val borderColor = lerp(
        start = MaterialTheme.colorScheme.outline,
        stop = MaterialTheme.colorScheme.primary,
        fraction = 0.35f,
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 2.dp)
            .padding(end = 4.dp),
    ) {
        val isCompactWidth = maxWidth < 320.dp
        val mascotSize = if (isCompactWidth) 60.dp else 68.dp
        val bubbleStartPadding = if (isCompactWidth) 22.dp else 26.dp
        val contentStartPadding = if (isCompactWidth) 34.dp else 40.dp
        val contentEndPadding = if (trailingIcon != null) 12.dp else 16.dp
        val trailingIconSize = if (isCompactWidth) 16.dp else 18.dp

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = bubbleStartPadding, end = 2.dp, top = 4.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            shape = RoundedCornerShape(20.dp),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 60.dp)
                    .padding(
                        start = contentStartPadding,
                        end = contentEndPadding,
                        top = 14.dp,
                        bottom = 14.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    supportingText?.takeIf { it.isNotBlank() }?.let { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = AekkimThemeTokens.colors.textSubtle,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                trailingIcon?.let { icon ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .then(
                                if (onTrailingIconClick != null) {
                                    Modifier.clickable(onClick = onTrailingIconClick)
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = trailingContentDescription,
                            modifier = Modifier.size(trailingIconSize),
                            tint = AekkimThemeTokens.colors.textMuted,
                        )
                    }
                }
            }
        }

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("file:///android_asset/sherlock_hamzzi.png")
                .crossfade(true)
                .build(),
            contentDescription = "대시보드 햄스터",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(mascotSize)
                .padding(top = 2.dp),
            contentScale = ContentScale.Fit,
        )
    }
}
