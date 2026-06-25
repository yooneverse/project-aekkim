package com.ssafy.e106.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ssafy.e106.R

private val DefaultTypography = Typography()

val PretendardFontFamily = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
    Font(R.font.pretendard_black, FontWeight.Black),
)

private fun TextStyle.withPretendard(): TextStyle = copy(fontFamily = PretendardFontFamily)

val AekkimTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = DefaultTypography.displayMedium.withPretendard(),
    displaySmall = DefaultTypography.displaySmall.withPretendard(),
    headlineLarge = DefaultTypography.headlineLarge.copy(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineMedium = DefaultTypography.headlineMedium.copy(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    headlineSmall = DefaultTypography.headlineSmall.withPretendard(),
    titleLarge = DefaultTypography.titleLarge.copy(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = DefaultTypography.titleMedium.copy(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = DefaultTypography.titleSmall.withPretendard(),
    bodyLarge = DefaultTypography.bodyLarge.copy(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = DefaultTypography.bodyMedium.copy(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = DefaultTypography.bodySmall.withPretendard(),
    labelLarge = DefaultTypography.labelLarge.copy(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = DefaultTypography.labelMedium.withPretendard(),
    labelSmall = DefaultTypography.labelSmall.copy(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
)
