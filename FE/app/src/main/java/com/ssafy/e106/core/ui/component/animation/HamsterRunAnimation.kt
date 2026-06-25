package com.ssafy.e106.core.ui.component.animation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 햄찌 달리기 애니메이션 Composable.
 *
 * ## 프레임 이미지 준비 방법
 * `assets/animations/hamster_run/` 폴더에 달리기 동작의 프레임 이미지를 넣어 주세요.
 *
 * - **권장 형식**: WebP (용량 효율적)
 * - **권장 크기**: 256×256px ~ 512×512px
 * - **파일 이름 규칙**: `frame_00.webp`, `frame_01.webp`, ... (정렬 순서대로 재생)
 * - **권장 프레임 수**: 8~12 프레임 (달리기 사이클 1회)
 *
 * @param modifier Composable modifier
 * @param size 표시 크기
 * @param frameIntervalMs 프레임 간 간격 (ms). 기본값 80ms ≈ 12.5fps
 */
@Composable
fun HamsterRunAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    frameIntervalMs: Long = 80L,
) {
    SpriteAnimation(
        assetFolder = HAMSTER_RUN_ASSET_FOLDER,
        modifier = modifier,
        size = size,
        frameIntervalMs = frameIntervalMs,
        contentDescription = "달리는 햄스터",
    )
}

private const val HAMSTER_RUN_ASSET_FOLDER = "animations/hamster_run"
