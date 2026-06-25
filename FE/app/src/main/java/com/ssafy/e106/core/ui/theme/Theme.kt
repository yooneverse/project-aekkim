package com.ssafy.e106.core.ui.theme

import android.app.Activity
import androidx.compose.runtime.Immutable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = TextOnPrimary,
    primaryContainer = PrimaryPressed,
    onPrimaryContainer = TextOnPrimary,
    secondary = TextPrimary,
    onSecondary = White,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSubtle,
    outline = Border,
    outlineVariant = Border,
    tertiary = Success,
    onTertiary = White,
    surfaceTint = Color.Transparent,
)

@Immutable
data class AekkimSemanticColors(
    val bgDefault: Color = Background,
    val bgSurface: Color = Surface,
    val bgMuted: Color = SurfaceMuted,
    val textDefault: Color = TextPrimary,
    val textSubtle: Color = TextSubtle,
    val textMuted: Color = TextMuted,
    val textOnPrimary: Color = TextOnPrimary,
    val borderDefault: Color = Border,
    val borderStrong: Color = BorderStrong,
    val actionPrimary: Color = Primary,
    val actionPrimaryPressed: Color = PrimaryPressed,
    val statusSuccess: Color = Success,
    val statusProgress: Color = Primary,
    val statusDisabled: Color = Disabled,
)

val LocalSemanticColors = staticCompositionLocalOf { AekkimSemanticColors() }

object AekkimThemeTokens {
    val colors: AekkimSemanticColors
        @Composable
        get() = LocalSemanticColors.current
}

/**
 * Aekkim branded theme — Light Mode only (Design System v2).
 *
 * [darkTheme] and [dynamicColor] are kept for API compatibility but
 * intentionally unused: the design system mandates a single branded
 * light-mode palette.
 */
@Composable
fun AekkimTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = LightColorScheme
    val semanticColors = AekkimSemanticColors()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    CompositionLocalProvider(
        LocalSpacing provides AekkimSpacing(),
        LocalSemanticColors provides semanticColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AekkimTypography,
            shapes = AekkimShapes,
            content = content,
        )
    }
}
