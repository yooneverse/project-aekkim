package com.ssafy.e106.feature.auth.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.ssafy.e106.core.ui.theme.LocalSpacing

enum class AuthHeroVariant {
    Display,
    Screen,
}

@Composable
fun AuthHeroSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    variant: AuthHeroVariant = AuthHeroVariant.Screen,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    textAlign: TextAlign = TextAlign.Start,
    topContent: (@Composable () -> Unit)? = null,
) {
    val spacing = LocalSpacing.current
    val titleStyle = when (variant) {
        AuthHeroVariant.Display -> MaterialTheme.typography.displayLarge
        AuthHeroVariant.Screen -> MaterialTheme.typography.headlineLarge
    }
    val subtitleStyle = when (variant) {
        AuthHeroVariant.Display -> MaterialTheme.typography.bodyLarge
        AuthHeroVariant.Screen -> MaterialTheme.typography.bodyMedium
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        topContent?.invoke()

        Column(verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
            Text(
                text = title,
                style = titleStyle,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = subtitle,
                style = subtitleStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
