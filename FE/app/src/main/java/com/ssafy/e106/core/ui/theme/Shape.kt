package com.ssafy.e106.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Aekkim shape tokens — mapped from Design System v2 radius scale.
 */
val AekkimShapes = Shapes(
    small = RoundedCornerShape(8.dp),       // radius-sm: 태그, 작은 배지
    medium = RoundedCornerShape(12.dp),     // radius-md: 입력폼, 작은 카드
    large = RoundedCornerShape(16.dp),      // radius-lg: 기본 버튼, 카드
    extraLarge = RoundedCornerShape(20.dp), // radius-xl: 강조 카드, 바텀시트 상단
)

/** radius-pill: pill 버튼, 칩 */
val PillShape = RoundedCornerShape(999.dp)
