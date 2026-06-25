package com.ssafy.e106.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Aekkim spacing tokens — mapped from Design System v2 (4pt grid).
 * Access via `LocalSpacing.current` inside any composable within AekkimTheme.
 */
@Immutable
data class AekkimSpacing(
    val space1: Dp = 4.dp,   // 미세 간격
    val space2: Dp = 8.dp,   // 아이콘-텍스트 간격
    val space3: Dp = 12.dp,  // 작은 그룹 간격
    val space4: Dp = 16.dp,  // 기본 내부 여백
    val space5: Dp = 20.dp,  // 카드 내부 여백 / 섹션 보조
    val space6: Dp = 24.dp,  // 섹션 간격
    val space7: Dp = 32.dp,  // 큰 블록 간격
    val space8: Dp = 40.dp,  // 큰 여백
    /** 화면 좌우 기본 패딩 (= 20dp / space5) */
    val screenHorizontalPadding: Dp = 20.dp,
    /** 카드 내부 기본 패딩 (= 16dp / space4) */
    val cardPadding: Dp = 16.dp,
    /** 섹션 간 기본 간격 (= 24dp / space6) */
    val sectionGap: Dp = 24.dp,
)

val LocalSpacing = staticCompositionLocalOf { AekkimSpacing() }
