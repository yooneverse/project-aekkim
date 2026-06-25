package com.ssafy.e106.core.ui.component.animation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest

/**
 * assets 폴더의 햄찌 탐정 SVG 이미지를 표시하는 Composable.
 *
 * Coil + SvgDecoder를 사용하여 SVG를 렌더링합니다.
 *
 * ```kotlin
 * HamsterImage(size = 200.dp)
 * ```
 *
 * @param modifier Composable modifier
 * @param size 표시 크기
 * @param contentDescription 접근성 설명
 */
@Composable
fun HamsterImage(
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    contentDescription: String? = "햄스터 탐정",
) {
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data("file:///android_asset/$HAMSTER_SVG_PATH")
            .decoderFactory(SvgDecoder.Factory())
            .build(),
    )

    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}

private const val HAMSTER_SVG_PATH = "sherlock_hamzzi.svg"
